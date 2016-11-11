package xap.test.performance

import akka.stream.KillSwitches
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.driver.core.utils.UUIDs
import com.websudos.util.testing._
import org.joda.time.DateTime
import xap.entity.ItemUpdate
import xap.service.ItemUpdateService
import xap.test.utils.{CassandraSpec, WithGuiceInjectorAndImplicites}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class PerformanceTest extends CassandraSpec with WithGuiceInjectorAndImplicites {

  val ItemUpdateService = injector.getInstance(classOf[ItemUpdateService])

  val sharedKillSwitch = KillSwitches.shared("shared-kill-switch")

  override def beforeAll(): Unit = {
    Await.result(database.autocreate().future(), 5.seconds)
  }

  override def afterAll(): Unit = {
    Await.result(database.autotruncate().future(), 5.seconds)
  }

  implicit object ItemUpdateGenerator extends Sample[ItemUpdate] {
    override def sample: ItemUpdate = {
      ItemUpdate(
        UUIDs.timeBased(),
        gen[Long],
        Option(UUIDs.timeBased()),
        gen[DateTime],
        gen[DateTime],
        gen[String]
      )
    }
  }

  val maxId = 100000
  val timeout = 20 second

  val storeTitle = s"store $maxId records in $timeout"
  it should storeTitle in {

    val f = Source.fromIterator(() => Iterator.range(1, maxId))
      .via(sharedKillSwitch.flow)
      .map(i =>
        gen[ItemUpdate].copy(itemId=i)
      ).mapAsync(1000) {i =>
        val f = ItemUpdateService.saveOrUpdate(i)

        f.onFailure({case e => sharedKillSwitch.abort(e)})
        f
      } runWith Sink.ignore

    Await.result(f, timeout)
  }

  val readTimeout = 20 second

  val readTitle = s"random read $maxId records in $readTimeout"
  it should readTitle in {
    val rnd = new Random()
    val f = Source.fromIterator(() => Iterator.range(1, maxId))
      .via(sharedKillSwitch.flow)
      .mapAsync(1000) {i =>
        val f = ItemUpdateService.getByItemId(rnd.nextInt(maxId))
        f.onFailure({case e => sharedKillSwitch.abort(e)})
        f
      } runWith Sink.ignore

    Await.result(f, readTimeout)
  }
}
