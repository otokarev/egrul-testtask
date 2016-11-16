package xap.test.service

import com.datastax.driver.core.utils.UUIDs
import com.websudos.util.testing._
import org.joda.time.{DateTime, DateTimeZone}
import xap.entity.{BatchWithItemUpdates, Item}
import xap.service.{BatchWithItemUpdatesService, ItemService, ItemUpdateService}
import xap.test.utils.{CassandraSpec, WithGuiceInjectorAndImplicites}
import xap.util.LoremIpsum

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class BatchTest extends CassandraSpec with WithGuiceInjectorAndImplicites {

  val ItemUpdateService = injector.getInstance(classOf[ItemUpdateService])
  val ItemService = injector.getInstance(classOf[ItemService])
  val BatchWithItemUpdatesService = injector.getInstance(classOf[BatchWithItemUpdatesService])

  override def beforeAll(): Unit = {
    Await.result(database.autocreate().future(), 5 seconds)
  }

  override def afterAll(): Unit = {
    Await.result(database.autotruncate().future(), 10 seconds)
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

  val itemIdRange = 1 to 10000

  val itemIds = ListBuffer(itemIdRange)
  val rnd = new Random()
  val txnPerDay = 20 to 50
  val daysNum = 10

  val startDateTime = DateTime.now(DateTimeZone.UTC).withTime(0, 0, 0, 0)

  "Test items" should "be inserted into C*" in {
    generateTestItems()
  }

  "Batches" should "be created into C*" in {
    generateBatches()
  }

  "Batches" should "be retrieved" in {
    val r = BatchWithItemUpdatesService.getByDateTimeRange((startDateTime, startDateTime.plusDays(daysNum)))
    // TODO: generate XML
  }

  def generateBatches() = {

    // Loop 9 time periods
    val result = (0 until daysNum).toList
      // Calculate ranges for every time period
      .map { i => (startDateTime.plusDays(i), startDateTime.plusDays(i).withTime(23, 59, 59, 999)) }
      // Loop periods
      .foreach { r =>
        val itemUpdates = Await.result(ItemUpdateService.getByDateTimeRange(r), 1 second)
        BatchWithItemUpdatesService.saveOrUpdate(BatchWithItemUpdates(UUIDs.timeBased(), r._2, itemUpdates))
        Await.ready(for {
          itemUpdates <- ItemUpdateService.getByDateTimeRange(r)
          batchWithItemUpdatesRs <- BatchWithItemUpdatesService.saveOrUpdate(BatchWithItemUpdates(UUIDs.timeBased(), r._2, itemUpdates))
        } yield batchWithItemUpdatesRs, 10 second)
      }
  }

  def generateTestItems() = {

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
        val item = Item(rnd.nextInt(itemIdRange.last), dateTime, LoremIpsum.getRandomNumberOfWords(1000 to 5000))
        Await.ready(ItemService.saveOrUpdate(item), 1 second)
      }
    }
  }


}
