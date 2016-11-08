package xap.service

import com.websudos.phantom.dsl._
import xap.database.{DatabaseProvider, Embedded3rdPartyDatabase, EmbeddedDatabase, ProductionDatabase}
import xap.entity.BatchWithItems

import scala.concurrent.Future

trait BatchWithItemsService extends DatabaseProvider {

  /**
    * Find batch by Id
    * @param id Item's ID that is unique in our database
    * @return
    */
  def getBatchById(id: UUID): Future[Option[BatchWithItems]] = {
    val batchF = database.batchesModel.getById(id)
    val batchedItemsF = database.itemsByBatchIdsModel.getByBatchId(id)

    val f = for {
      batchedItems <- batchedItemsF
      items <- Future.traverse(batchedItems)(a => database.itemsModel.getByIdList(a.id))
      batch <- batchF
    } yield (items.flatten, batch)

    f map {
      case Tuple2(items, Some(batch)) => Option(BatchWithItems(batch.id, batch.createdAt, items))
      case _ => None
    }
  }

  /**
    * Save an item in both tables
    *
    * @param batchWithItems BatchWithItems
    * @return
    */
  def saveOrUpdate(batchWithItems: BatchWithItems): Future[ResultSet] = {
    val itemsF = Future.sequence(batchWithItems.items.map {i =>
      ItemService.saveOrUpdate(i.copy(batchId = batchWithItems.id))
    })

    val batchF = BatchService.saveOrUpdate(batchWithItems)

    for {
      items <- itemsF
      batch <- batchF
    } yield batch
  }

}

/**
  * Let available a singleton instance of this service class, to prevent unnecessary instances
  */
object BatchWithItemsService extends BatchService with ProductionDatabase
object TestBatchWithItemsService extends BatchService with EmbeddedDatabase
object Test3rdPartyBatchWithItemsService extends BatchService with Embedded3rdPartyDatabase

