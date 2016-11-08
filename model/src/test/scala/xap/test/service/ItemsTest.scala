package xap.test.service

import com.datastax.driver.core.utils.UUIDs
import com.websudos.util.testing._
import org.joda.time.DateTime
import org.scalatest.time.{Millis, Seconds, Span}
import xap.connector.Connector
import xap.database.EmbeddedDatabase
import xap.entity.Item
import xap.service.ItemService
import xap.test.utils.CassandraSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ItemsTest extends CassandraSpec with EmbeddedDatabase with Connector.testConnector.Connector {

  object ItemService extends ItemService with EmbeddedDatabase

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
        UUIDs.timeBased,
        12345,
        UUIDs.timeBased(),
        gen[DateTime],
        gen[DateTime],
        gen[String]
      )
    }
  }

  "A Item" should "be inserted into cassandra" in {
    val sample = gen[Item]
    val future = ItemService.saveOrUpdate(sample)

    whenReady(future) { result =>
      result isExhausted() shouldBe true
      result wasApplied() shouldBe true
      ItemService.delete(sample)
    }
  }

  it should "find an item by id" in {
    val sample = gen[Item]

    val chain = for {
      store <- ItemService.saveOrUpdate(sample)
      get <- ItemService.getItemById(sample.id)
      delete <- ItemService.delete(sample)
    } yield get

    whenReady(chain) { res =>
      res shouldBe defined
      ItemService.delete(sample)
    }
  }

  it should "find items by itemId" in {
    val sample = gen[Item]
    val sample2 = gen[Item]
    val sample3 = gen[Item]

    val future = for {
      f1 <- ItemService.saveOrUpdate(sample.copy(payload = "Toxicity"))
      f2 <- ItemService.saveOrUpdate(sample2.copy(payload = "Aerials"))
      f3 <- ItemService.saveOrUpdate(sample3.copy(payload = "Chop Suey"))
    } yield (f1, f2, f3)

    whenReady(future) { insert =>
      val itemsByItemId = ItemService.getItemsByItemId(12345)
      whenReady(itemsByItemId) { searchResult =>
        searchResult shouldBe a [List[_]]
        searchResult should have length 3
        ItemService.delete(sample)
        ItemService.delete(sample2)
        ItemService.delete(sample3)
      }
    }
  }

  it should "be updated into cassandra" in {
    val sample = gen[Item]
    val updatedPayload = gen[String]

    val chain = for {
      store <- ItemService.saveOrUpdate(sample)
      unmodified <- ItemService.getItemById(sample.id)
      store <- ItemService.saveOrUpdate(sample.copy(payload = updatedPayload))
      modified <- ItemService.getItemById(sample.id)
    } yield (unmodified, modified)

    whenReady(chain) {
      case (initial, modified) =>
        initial shouldBe defined
        initial.value.payload shouldEqual sample.payload

        modified shouldBe defined
        modified.value.payload shouldEqual updatedPayload

        ItemService.delete(modified.get)
    }
  }

}