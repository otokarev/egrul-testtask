package xap.test.stream

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import com.datastax.driver.core.utils.UUIDs
import com.websudos.phantom.reactivestreams._
import com.websudos.util.testing._
import org.joda.time.DateTime
import xap.connector.Connector
import xap.database.ProductionDatabase
import xap.entity.Item
import xap.service.ItemService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ItemsStreaming extends ProductionDatabase with Connector.connector.Connector {

  def main(args: Array[String]) {

    val truncate = database.autotruncate().future()

    val insert = Future.sequence(List(
      ItemService.saveOrUpdate(Item(UUIDs.timeBased(), gen[Long], UUIDs.timeBased(), gen[DateTime], gen[DateTime], gen[String])),
      ItemService.saveOrUpdate(Item(UUIDs.timeBased(), gen[Long], UUIDs.timeBased(), gen[DateTime], gen[DateTime], gen[String])),
      ItemService.saveOrUpdate(Item(UUIDs.timeBased(), gen[Long], UUIDs.timeBased(), gen[DateTime], gen[DateTime], gen[String]))
    ))

    val f = for {
      f1 <- truncate
      f2 <- insert
    } yield f2

    Await.result(f, 10.seconds)

    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()

    Source
      .fromPublisher(database.itemsModel.publisher())
      .via(Flow[Item].map(item => s"Id: ${item.id} - ItemId: ${item.itemId} - CreationDate: ${item.modifiedAt}"))
      .to(Sink.foreach(println))
      .run()
  }
}