package xap.test.service

import com.datastax.driver.core.utils.UUIDs
import com.websudos.phantom.dsl.ResultSet
import com.websudos.util.testing._
import org.joda.time.DateTime
import xap.connector.Connector
import xap.database.EmbeddedDatabase
import xap.entity.Item
import xap.test.utils.CassandraSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ItemsTest extends CassandraSpec with EmbeddedDatabase with Connector.testConnector.Connector {

  override def beforeAll(): Unit = {
    Await.result(database.autocreate().future(), 5.seconds)
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
    val future = this.store(sample)

    whenReady(future) { result =>
      result isExhausted() shouldBe true
      result wasApplied() shouldBe true
      this.drop(sample)
    }
  }

  it should "find an item by id" in {
    val sample = gen[Item]

    val chain = for {
      store <- this.store(sample)
      get <- database.itemsModel.getById(sample.id)
      delete <- this.drop(sample)
    } yield get

    whenReady(chain) { res =>
      res shouldBe defined
      this.drop(sample)
    }
  }

  it should "find items by itemId" in {
    val sample = gen[Item]
    val sample2 = gen[Item]
    val sample3 = gen[Item]

    val future = for {
      f1 <- this.store(sample.copy(payload = "Toxicity"))
      f2 <- this.store(sample2.copy(payload = "Aerials"))
      f3 <- this.store(sample3.copy(payload = "Chop Suey"))
    } yield (f1, f2, f3)

    whenReady(future) { insert =>
      val itemsByItemId = database.itemsByItemIdsModel.getByItemId(12345)
      whenReady(itemsByItemId) { searchResult =>
        searchResult shouldBe a [List[_]]
        searchResult should have length 3
        this.drop(sample)
        this.drop(sample2)
        this.drop(sample3)
      }
    }
  }

  it should "be updated into cassandra" in {
    val sample = gen[Item]
    val updatedPayload = gen[String]

    val chain = for {
      store <- this.store(sample)
      unmodified <- database.itemsModel.getById(sample.id)
      store <- this.store(sample.copy(payload = updatedPayload))
      modified <- database.itemsModel.getById(sample.id)
    } yield (unmodified, modified)

    whenReady(chain) {
      case (initial, modified) =>
        initial shouldBe defined
        initial.value.payload shouldEqual sample.payload

        modified shouldBe defined
        modified.value.payload shouldEqual updatedPayload

        this.drop(modified.get)
    }
  }

  /**
    * Utility method to store into both tables
    *
    * @param item the item to be inserted
    * @return a [[Future]] of [[ResultSet]]
    */
  private def store(item: Item): Future[ResultSet] = {
    for {
      byId <- database.itemsModel.store(item)
      byItemId <- database.itemsByItemIdsModel.store(item)
    } yield byItemId
  }

  /**
    * Utility method to delete into both tables
    *
    * @param item the item to be deleted
    * @return a [[Future]] of [[ResultSet]]
    */
  private def drop(item: Item) = {
    for {
      byID <- database.itemsModel.deleteById(item.id)
      byItemId <- database.itemsByItemIdsModel.deleteByItemIdAndId(item.itemId, item.id)
    } yield byItemId
  }
}