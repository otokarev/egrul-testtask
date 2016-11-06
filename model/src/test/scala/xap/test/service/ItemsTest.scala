package xap.test.service

import com.datastax.driver.core.utils.UUIDs
import com.websudos.util.testing._
import org.joda.time.DateTime
import org.scalatest.time.{Millis, Seconds, Span}
import xap.connector.Connector
import xap.database.EmbeddedDatabase
import xap.entity.Item
import xap.service.ItemsService
import xap.test.utils.CassandraSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ItemsTest extends CassandraSpec with EmbeddedDatabase with Connector.testConnector.Connector {

  object ItemsService extends ItemsService with EmbeddedDatabase

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(1, Seconds), interval = Span(20, Millis))

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
        12345,
        gen[DateTime],
        gen[String]
      )
    }
  }

  "A Item" should "be inserted into cassandra" in {
    val sample = gen[Item]
    val future = ItemsService.saveOrUpdate(sample)

    whenReady(future) { result =>
      result isExhausted() shouldBe true
      result wasApplied() shouldBe true
      ItemsService.delete(sample)
    }
  }

  it should "find an item by id" in {
    val sample = gen[Item]

    val chain = for {
      store <- ItemsService.saveOrUpdate(sample)
      get <- ItemsService.getItemById(sample.id)
      delete <- ItemsService.delete(sample)
    } yield get

    whenReady(chain) { res =>
      res shouldBe defined
      ItemsService.delete(sample)
    }
  }

  it should "find items by itemId" in {
    val sample = gen[Item]
    val sample2 = gen[Item]
    val sample3 = gen[Item]

    val future = for {
      f1 <- ItemsService.saveOrUpdate(sample.copy(payload = "Toxicity"))
      f2 <- ItemsService.saveOrUpdate(sample2.copy(payload = "Aerials"))
      f3 <- ItemsService.saveOrUpdate(sample3.copy(payload = "Chop Suey"))
    } yield (f1, f2, f3)

    whenReady(future) { insert =>
      val itemsByItemId = ItemsService.getItemsByItemId(12345)
      whenReady(itemsByItemId) { searchResult =>
        searchResult shouldBe a [List[_]]
        searchResult should have length 3
        ItemsService.delete(sample)
        ItemsService.delete(sample2)
        ItemsService.delete(sample3)
      }
    }
  }

  it should "be updated into cassandra" in {
    val sample = gen[Item]
    val updatedPayload = gen[String]

    val chain = for {
      store <- ItemsService.saveOrUpdate(sample)
      unmodified <- ItemsService.getItemById(sample.id)
      store <- ItemsService.saveOrUpdate(sample.copy(payload = updatedPayload))
      modified <- ItemsService.getItemById(sample.id)
    } yield (unmodified, modified)

    whenReady(chain) {
      case (initial, modified) =>
        initial shouldBe defined
        initial.value.payload shouldEqual sample.payload

        modified shouldBe defined
        modified.value.payload shouldEqual updatedPayload

        ItemsService.delete(modified.get)
    }
  }

}