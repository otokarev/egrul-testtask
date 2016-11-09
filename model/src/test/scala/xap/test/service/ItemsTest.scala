package xap.test.service

import com.websudos.util.testing._
import org.joda.time.DateTime
import org.scalatest.time.{Millis, Seconds, Span}
import xap.connector.Connector
import xap.database.EmbeddedDatabase
import xap.entity.{Item, ItemBase}
import xap.service.{ItemBaseService, ItemService}
import xap.test.utils.CassandraSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ItemsTest extends CassandraSpec with EmbeddedDatabase with Connector.testConnector.Connector {

  object ItemService extends ItemService with EmbeddedDatabase
  object ItemBaseService extends ItemBaseService with EmbeddedDatabase

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(1, Seconds), interval = Span(20, Millis))

  override def beforeAll(): Unit = {
    println("before all")
    Await.result(database.autocreate().future(), 5.seconds)
  }

  override def afterAll(): Unit = {
    println("after all")
    Await.result(database.autotruncate().future(), 5.seconds)
  }

  implicit object ItemGenerator extends Sample[Item] {
    override def sample: Item = {
      Item(
        gen[Long],
        Some(gen[DateTime]),
        gen[String]
      )
    }
  }

  val sample: Item = gen[Item]

  "An Item" should "be inserted into C*" in {
    val sample: Item = gen[Item]
    val future = ItemService.saveOrUpdate(sample)

    whenReady(future) { result =>
      result isExhausted() shouldBe true
      result wasApplied() shouldBe true
    }
  }

  "An Item" should "be readable from C*" in {
    val future = ItemService.getById(sample.id)

    whenReady(future) { result =>
      result.get shouldBe Some(sample.copy(at = None))
    }
  }

  "A BaseItem" should "be inserted into cassandra" in {
    val future = ItemBaseService.getById(sample.id)

    whenReady(future) { result =>
      result shouldBe Some(ItemBase(sample.id, sample.at.get))
    }
  }

}
