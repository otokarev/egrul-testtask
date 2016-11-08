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
import xap.entity.ItemUpdate
import xap.service.ItemUpdateService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ItemUpdatesStreaming extends ProductionDatabase with Connector.connector.Connector {

  def main(args: Array[String]) {

    val truncate = database.autotruncate().future()

    val insert = Future.sequence(List(
      ItemUpdateService.saveOrUpdate(ItemUpdate(UUIDs.timeBased(), gen[Long], UUIDs.timeBased(), gen[DateTime], gen[DateTime], gen[String])),
      ItemUpdateService.saveOrUpdate(ItemUpdate(UUIDs.timeBased(), gen[Long], UUIDs.timeBased(), gen[DateTime], gen[DateTime], gen[String])),
      ItemUpdateService.saveOrUpdate(ItemUpdate(UUIDs.timeBased(), gen[Long], UUIDs.timeBased(), gen[DateTime], gen[DateTime], gen[String]))
    ))

    val f = for {
      f1 <- truncate
      f2 <- insert
    } yield f2

    Await.result(f, 10.seconds)

    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()

    Source
      .fromPublisher(database.itemUpdatesModel.publisher())
      .via(Flow[ItemUpdate].map(itemUpdate => s"Id: ${itemUpdate.id} - ItemId: ${itemUpdate.itemId} - CreationDate: ${itemUpdate.modifiedAt}"))
      .to(Sink.foreach(println))
      .run()
  }
}