package xap.test.service

import com.websudos.util.testing._
import org.joda.time._
import org.scalatest.time.{Millis, Seconds, Span}
import xap.entity.{Item, ItemBase}
import xap.service.{ItemBaseService, ItemService}
import xap.test.utils.{CassandraSpec, WithGuiceInjectorAndImplicites}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class ItemsTest extends CassandraSpec with WithGuiceInjectorAndImplicites {

  val ItemService = injector.getInstance(classOf[ItemService])
  val ItemBaseService = injector.getInstance(classOf[ItemBaseService])

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(1, Seconds), interval = Span(20, Millis))

  override def beforeAll(): Unit = {
    println("before all")
    Await.result(database.autocreate().future(), 5 seconds)
  }

  override def afterAll(): Unit = {
    println("after all")
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

  val sample: Item = gen[Item]

  "An Item" should "be inserted into C*" in {
    val future = ItemService.saveOrUpdate(sample)

    whenReady(future) { result =>
      result isExhausted() shouldBe true
      result wasApplied() shouldBe true
    }
  }

  "An Item" should "be readable from C*" in {
    val future = ItemService.getById(sample.id)

    whenReady(future) { result =>
      result shouldBe Some(sample)
    }
  }

  "A BaseItem" should "be inserted into cassandra" in {
    val future = ItemBaseService.getById(sample.id)

    whenReady(future) { result =>
      result shouldBe Some(ItemBase(sample.id, sample.at))
    }
  }

  "The latest version of Item" should "be readable from C*" in {
    var time = DateTime.now(DateTimeZone.UTC)

    // TODO: We can insert Items one by one only.. It's a problem :-/
    val samples = (1 to 100).toList.map { a =>
      time = time.plusSeconds(3)
      Item(5000, time, gen[String])
    }

    samples map {a => Await.result(ItemService.saveOrUpdate(a), 1 second)}

    val result = Await.result(ItemService.getById(5000), 1 second)

    println(samples.last)
    result shouldBe Some(samples.last)
  }
}
