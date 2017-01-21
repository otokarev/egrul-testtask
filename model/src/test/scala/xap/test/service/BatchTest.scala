package xap.test.service

import java.util.UUID

import com.datastax.driver.core.utils.UUIDs
import com.typesafe.config.ConfigFactory
import com.websudos.util.testing._
import org.joda.time.{DateTime, DateTimeZone}
import xap.entity.{BatchWithItemUpdates, Item, ItemUpdate}
import xap.service.{BatchWithItemUpdatesService, ItemService, ItemUpdateService}
import xap.test.utils.{CassandraSpec, ModuleForTest3rdPartyEmulation, WithGuiceInjectorAndImplicites}
import xap.util.LoremIpsum

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Random, Success, Try}
import scala.xml.XML

class BatchTest extends CassandraSpec with WithGuiceInjectorAndImplicites {

  private val ItemUpdateService = injector.getInstance(classOf[ItemUpdateService])
  private val ItemService = injector.getInstance(classOf[ItemService])
  private val BatchWithItemUpdatesService = injector.getInstance(classOf[BatchWithItemUpdatesService])

  private val archiveDir = System.getProperty("user.dir") + '/' + ConfigFactory.load().getString("xmlarchiveparser.dir")
  private val archiveFilePattern = """batch_archive_daily-.*\.zip""".r
  private val batchFilePattern = """batch-.*\.XML""".r

  override def beforeAll(): Unit = {
    Await.result(database.autocreate().future(), 5 seconds)
    ThirdPartyServerContextEmulation.beforeAll()
  }

  override def afterAll(): Unit = {
    Await.result(database.autotruncate().future(), 10 seconds)
    ThirdPartyServerContextEmulation.afterAll()
  }

  implicit object ItemGenerator extends Sample[Item] {
    override def sample: Item = {
      Item(
        gen[Long],
        new DateTime(DateTimeZone.UTC),
        gen[String]
      )
    }
  }

  private val itemIdRange = 1 to 100

  private val itemIds = ListBuffer(itemIdRange)
  private val rnd = new Random()
  private val txnPerDay = 10 to 30
  private val daysNum = 20
  private val loremIpsumWordsNumRange = 2000 to 3000
  private val daysPerBatch = 5 // keep it > 1

  private val startDateTime = DateTime.now(DateTimeZone.UTC).withTime(0, 0, 0, 0)

  "Test items" should "be inserted into C*" in {
    val count = generateTestItems()
    println(s"Stored Updates: $count")
  }

  "Batches" should "be created into C*" in {
    generateBatches()
  }

  "Batches ZIP-archives" should "be generated" in {
    import java.io.{FileOutputStream, _}
    import java.util.zip.{ZipEntry, ZipOutputStream}

    var count = 0

    // clean the directory from old batch archives
    new File(archiveDir).listFiles()
      .toList.filter(archiveFilePattern findFirstIn _.getName isDefined).foreach(_.delete())

    val batchListProcessor = (l: (List[BatchWithItemUpdates], DateTime)) => {
      val zipFile = new FileOutputStream(archiveDir + "/batch_archive_daily-" + l._2.toString("YYYY-MM-dd") + ".zip")

      val zip = new ZipOutputStream(zipFile)

      l._1.foreach { b =>
        val xmlFile = "batch-" + b.createdAt.toString("YYYY-MM-dd") + ".XML"
        zip.putNextEntry(new ZipEntry(xmlFile))

        val xml = <batch id={ b.id.toString } createdAt={ b.createdAt.toString() }>
            { b.itemUpdates.map { i =>
              count += 1
              <item id={ i.itemId.toString } createdAt={ i.createdAt.toString() } modifiedAt={ i.modifiedAt.toString() }>
                <payload>{ i.payload.toString }</payload>
              </item>
            } }
          </batch>

        zip.write(xml.toString().getBytes)
        zip.closeEntry()
      }

      zip.close()
      zipFile.close()
    }

    traverseBatches(batchListProcessor)

    println(s"Zipped Updates: $count")
  }

  "ZIP-archives" should "be read, parsed and loaded" in {
    import java.io._
    import java.util.zip.ZipFile

    import scala.collection.JavaConversions._

    val archiveFiles = new File(archiveDir).listFiles()
      .filter(_.isFile).toList.filter(archiveFilePattern findFirstIn _.getName isDefined).map(_.getName)

    archiveFiles.foreach { file =>
      val path = archiveDir + "/" + file
      val zipFile = Try { new ZipFile(path) } match {
        case Success(a) => a
        case Failure(e) => throw new Exception(s"Cannot open file: `$path`")
      }
      zipFile.entries.toList.filter(batchFilePattern findFirstIn _.getName isDefined).foreach { entry =>
        val source = Source.fromInputStream(zipFile.getInputStream(entry))
        val xmlStr = source.mkString
        source.close()
        val elem = XML.loadString(xmlStr)

        val batchId = UUID.fromString((elem \@ "id").toString)

        (elem \ "item") foreach { item =>
          Await.result(ThirdPartyServerContextEmulation.getItemUpdateService.saveOrUpdate(ItemUpdate(
            UUIDs.timeBased(),
            (item \@ "id").toLong,
            Some(batchId),
            DateTime.parse((item \@ "createdAt").toString),
            DateTime.parse((item \@ "modifiedAt").toString),
            (item \ "payload").text
          )), 1 seconds)
        }
      }
      zipFile.close()
    }
  }

  "Object stored in source DB and target" should "be the same" in {
    //TODO: Amount of UpdateItems must be the same
    traverseBatches((l) => {
      l._1.foreach(b => {
        b.itemUpdates.foreach(item => {
          val items = Await.result(ThirdPartyServerContextEmulation.getItemUpdateService.getByItemId(item.itemId), 1 seconds)

          items.count(_.modifiedAt == item.modifiedAt) shouldBe 1
        })
      })
    })

    itemIdRange.map({itemId => (
        Await.result(ItemUpdateService.getLastForItemId(itemId), 1 seconds),
        Await.result(ThirdPartyServerContextEmulation.getItemUpdateService.getLastForItemId(itemId), 1 seconds)
    )}).foreach(a => {
      ((a._1.isDefined && a._2.isDefined) || (a._1.isEmpty && a._2.isEmpty)) shouldBe true

      if (a._1.isDefined) {
        val uid = gen[UUID]
        // Compare the same item stored on two different spots.
        //   reset id to the same value, it differs on different spots
        (a._1.get.copy(id=uid) == a._2.get.copy(id=uid)) shouldBe true
      }

    })


  }

  def traverseBatches(f: ((List[BatchWithItemUpdates], DateTime)) => Unit): Unit = {
    val r = (0 until daysNum).toList.grouped(daysPerBatch).toList
      .map { list =>
        (startDateTime.plusDays(list.head), startDateTime.plusDays(list.last).withTime(23, 59, 59, 999))
      }
      .map { a =>
        BatchWithItemUpdatesService.getByDateTimeRange((a._1, a._2)).map(b => (b, a._2))
      }

    Await.result(Future.sequence(r).map { r: List[(List[BatchWithItemUpdates], DateTime)] => r.foreach(f(_)) }, 100 seconds)
  }

  def generateBatches(): Unit = {

    // Loop 9 time periods
    val result = (0 until daysNum).grouped(daysPerBatch).toList
      // Calculate ranges for every time period
      .map { list =>
        (startDateTime.plusDays(list.head), startDateTime.plusDays(list.last).withTime(23, 59, 59, 999))
      }
      // Loop periods
      .foreach { r =>
        Await.result(for {
          itemUpdates <- ItemUpdateService.getByDateTimeRange(r)
          batchWithItemUpdatesRs <- BatchWithItemUpdatesService.saveOrUpdate(BatchWithItemUpdates(UUIDs.timeBased(), r._2, itemUpdates))
        } yield batchWithItemUpdatesRs, 100 second)
      }
  }

  def generateTestItems(): Int = {
    var count = 0
    // Loop 9 time periods
    (0 until daysNum).toList
      // Calculate ranges for every time period
      .map { i => (startDateTime.plusDays(i), startDateTime.plusDays(i).withTime(23, 59, 59, 999)) }
      // Loop periods
      .foreach { r =>
        // Loop transactions per period
        (1 to {
          txnPerDay.start + rnd.nextInt(txnPerDay.length)
        }).foreach { a =>
          val dateTime = r._1.plusMillis(rnd.nextInt((r._2.getMillis - r._1.getMillis).toInt))
          val item = Item(rnd.nextInt(itemIdRange.last), dateTime, LoremIpsum.getRandomNumberOfWords(loremIpsumWordsNumRange))
          Await.ready(ItemService.saveOrUpdate(item), 1 second)
          count += 1
        }
      }

    count
  }
}

object ThirdPartyServerContextEmulation {

  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import com.datastax.driver.core.Session
  import com.google.inject.{Guice, Injector}
  import com.websudos.phantom.connectors.KeySpace
  import com.websudos.phantom.dsl.KeySpaceDef
  import xap.database.ModelsDatabase

  val injector: Injector = Guice.createInjector(new ModuleForTest3rdPartyEmulation)
  val database: ModelsDatabase = injector.getInstance(classOf[ModelsDatabase])
  val connector: KeySpaceDef = injector.getInstance(classOf[KeySpaceDef])
  implicit val keySpace = KeySpace(connector.name)
  implicit val session: Session = connector.session

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  def getItemUpdateService: ItemUpdateService = injector.getInstance(classOf[ItemUpdateService])
  def getItemService: ItemService = injector.getInstance(classOf[ItemService])
  def getBatchWithItemUpdatesService: BatchWithItemUpdatesService = injector.getInstance(classOf[BatchWithItemUpdatesService])

  def beforeAll(): Unit = {
    Await.result(database.autocreate().future(), 5 seconds)
  }

  def afterAll(): Unit = {
    Await.result(database.autotruncate().future(), 10 seconds)
  }
}
