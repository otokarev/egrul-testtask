package xap.test.performance

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches}
import com.datastax.driver.core.utils.UUIDs
import com.websudos.util.testing._
import org.joda.time.DateTime
import xap.connector.Connector
import xap.database.EmbeddedDatabase
import xap.entity.Item
import xap.service.ItemsService
import xap.test.utils.CassandraSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class PerformanceTest extends CassandraSpec with EmbeddedDatabase with Connector.testConnector.Connector {

  object ItemsService extends ItemsService with EmbeddedDatabase

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  val sharedKillSwitch = KillSwitches.shared("shared-kill-switch")

  override def beforeAll(): Unit = {
    Await.result(database.autocreate().future(), 5.seconds)
  }

  override def afterAll(): Unit = {
    Await.result(database.autotruncate().future(), 5.seconds)
  }

  implicit object ItemGenerator extends Sample[Item] {
    override def sample: Item = {
      Item(
        UUIDs.timeBased(),
        gen[Long],
        gen[DateTime],
        gen[String]
      )
    }
  }

  val maxId = 100000
  val timeout = 10 second

  val storeTitle = s"store $maxId records in $timeout"
  it should storeTitle in {

    val f = Source.fromIterator(() => Iterator.range(1, maxId))
      .via(sharedKillSwitch.flow)
      .map(i =>
        gen[Item].copy(itemId=i)
      ).mapAsync(1000) {i =>
        val f = ItemsService.saveOrUpdate(i)

        f.onFailure({case e => sharedKillSwitch.abort(e)})
        f
      } runWith Sink.ignore

    Await.result(f, timeout)
  }

  val readTimeout = 15 second

  val readTitle = s"random read $maxId records in $readTimeout"
  it should readTitle in {
    val rnd = new Random()
    val f = Source.fromIterator(() => Iterator.range(1, maxId))
      .via(sharedKillSwitch.flow)
      .mapAsync(1000) {i =>
        val f = ItemsService.getItemsByItemId(rnd.nextInt(maxId))
        f.onFailure({case e => sharedKillSwitch.abort(e)})
        f
      } runWith Sink.ignore

    Await.result(f, readTimeout)
  }
}
