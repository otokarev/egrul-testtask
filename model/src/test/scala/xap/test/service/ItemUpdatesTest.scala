package xap.test.service

import com.datastax.driver.core.utils.UUIDs
import com.websudos.util.testing._
import org.joda.time.DateTime
import org.scalatest.time.{Millis, Seconds, Span}
import xap.connector.Connector
import xap.database.EmbeddedDatabase
import xap.entity.ItemUpdate
import xap.service.ItemUpdateService
import xap.test.utils.CassandraSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ItemUpdatesTest extends CassandraSpec with EmbeddedDatabase with Connector.testConnector.Connector {

  object ItemUpdateService extends ItemUpdateService with EmbeddedDatabase

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(1, Seconds), interval = Span(20, Millis))

  override def beforeAll(): Unit = {
    Await.result(database.autocreate().future(), 5.seconds)
  }

  override def afterAll(): Unit = {
    Await.result(database.autotruncate().future(), 5.seconds)
  }

  implicit object ItemUpdateGenerator extends Sample[ItemUpdate] {
    override def sample: ItemUpdate = {
      ItemUpdate(
        UUIDs.timeBased,
        12345,
        UUIDs.timeBased(),
        gen[DateTime],
        gen[DateTime],
        gen[String]
      )
    }
  }

  "A ItemUpdate" should "be inserted into cassandra" in {
    val sample = gen[ItemUpdate]
    val future = ItemUpdateService.saveOrUpdate(sample)

    whenReady(future) { result =>
      result isExhausted() shouldBe true
      result wasApplied() shouldBe true
      ItemUpdateService.delete(sample)
    }
  }

  it should "find an itemUpdate by id" in {
    val sample = gen[ItemUpdate]

    val chain = for {
      store <- ItemUpdateService.saveOrUpdate(sample)
      get <- ItemUpdateService.getItemUpdateById(sample.id)
      delete <- ItemUpdateService.delete(sample)
    } yield get

    whenReady(chain) { res =>
      res shouldBe defined
      ItemUpdateService.delete(sample)
    }
  }

  it should "find itemUpdates by itemId" in {
    val sample = gen[ItemUpdate]
    val sample2 = gen[ItemUpdate]
    val sample3 = gen[ItemUpdate]

    val future = for {
      f1 <- ItemUpdateService.saveOrUpdate(sample.copy(payload = "Toxicity"))
      f2 <- ItemUpdateService.saveOrUpdate(sample2.copy(payload = "Aerials"))
      f3 <- ItemUpdateService.saveOrUpdate(sample3.copy(payload = "Chop Suey"))
    } yield (f1, f2, f3)

    whenReady(future) { insert =>
      val itemUpdatesByItemId = ItemUpdateService.getItemUpdatesByItemId(12345)
      whenReady(itemUpdatesByItemId) { searchResult =>
        searchResult shouldBe a [List[_]]
        searchResult should have length 3
        ItemUpdateService.delete(sample)
        ItemUpdateService.delete(sample2)
        ItemUpdateService.delete(sample3)
      }
    }
  }

  it should "be updated into cassandra" in {
    val sample = gen[ItemUpdate]
    val updatedPayload = gen[String]

    val chain = for {
      store <- ItemUpdateService.saveOrUpdate(sample)
      unmodified <- ItemUpdateService.getItemUpdateById(sample.id)
      store <- ItemUpdateService.saveOrUpdate(sample.copy(payload = updatedPayload))
      modified <- ItemUpdateService.getItemUpdateById(sample.id)
    } yield (unmodified, modified)

    whenReady(chain) {
      case (initial, modified) =>
        initial shouldBe defined
        initial.value.payload shouldEqual sample.payload

        modified shouldBe defined
        modified.value.payload shouldEqual updatedPayload

        ItemUpdateService.delete(modified.get)
    }
  }

}