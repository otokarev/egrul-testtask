package xap.test.stream

import akka.stream.scaladsl._
import com.datastax.driver.core.utils.UUIDs
import com.websudos.phantom.reactivestreams._
import com.websudos.util.testing._
import org.joda.time.DateTime
import xap.entity.ItemUpdate
import xap.service.ItemUpdateService
import xap.test.utils.{CassandraSpec, WithGuiceInjectorAndImplicites}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ItemUpdatesStreamingTest extends CassandraSpec with WithGuiceInjectorAndImplicites {

    val ItemUpdateService = injector.getInstance(classOf[ItemUpdateService])

    val truncate = database.autotruncate().future()

    val insert = Future.sequence(List(
      ItemUpdateService.saveOrUpdate(ItemUpdate(UUIDs.timeBased(), gen[Long], Option(UUIDs.timeBased()), gen[DateTime], gen[DateTime], gen[String])),
      ItemUpdateService.saveOrUpdate(ItemUpdate(UUIDs.timeBased(), gen[Long], Option(UUIDs.timeBased()), gen[DateTime], gen[DateTime], gen[String])),
      ItemUpdateService.saveOrUpdate(ItemUpdate(UUIDs.timeBased(), gen[Long], Option(UUIDs.timeBased()), gen[DateTime], gen[DateTime], gen[String]))
    ))

    val f = for {
      f1 <- truncate
      f2 <- insert
    } yield f2

    Await.result(f, 10.seconds)

    Source
      .fromPublisher(database.itemUpdatesModel.publisher())
      .via(Flow[ItemUpdate].map(itemUpdate => s"Id: ${itemUpdate.id} - ItemId: ${itemUpdate.itemId} - CreationDate: ${itemUpdate.modifiedAt}"))
      .to(Sink.foreach(println))
      .run()
}