package xap.service

import com.websudos.phantom.dsl._
import xap.database.{DatabaseProvider, Embedded3rdPartyDatabase, EmbeddedDatabase, ProductionDatabase}
import xap.entity.Item

import scala.concurrent.Future

trait ItemService extends DatabaseProvider {

  /**
    * Find items by Id
    * @param id Item's ID that is unique in our database
    * @return
    */
  def getItemById(id: UUID): Future[Option[Item]] = {
    database.itemsModel.getById(id)
  }

  /**
   * Find items by itemIds
   *
   * @param itemId Item's ID as it come from outside
   * @return
   */
  def getItemsByItemId(itemId: Long): Future[List[Item]] = {
    for {
      l1 <- database.itemsByItemIdsModel.getByItemId(itemId)
      l2 <- Future.traverse(l1)(a => database.itemsModel.getByIdList(a.id))
    } yield l2.flatten
  }

  /**
    * Find items by batchId
    *
    * @param batchId Batch's ID the items attached to
    * @return
    */
  def getItemsByBatchId(batchId: UUID): Future[List[Item]] = {
    for {
      l1 <- database.itemsByBatchIdsModel.getByBatchId(batchId)
      l2 <- Future.traverse(l1)(a => database.itemsModel.getByIdList(a.id))
    } yield l2.flatten
  }
  /**
   * Save an item in both tables
   *
   * @param item Item
   * @return
   */
  def saveOrUpdate(item: Item): Future[ResultSet] = {
    val byIdF = database.itemsModel.store(item)
    val byItemIdF = database.itemsByItemIdsModel.store(item)
    val byBatchIdF = database.itemsByBatchIdsModel.store(item)

    for {
      byId <- byIdF
      byItemId <- byItemIdF
      byBatchId <- byBatchIdF
    } yield byItemId
  }

  /**
   * Delete an item in both tables
   *
   * @param item Item
   * @return
   */
  def delete(item: Item): Future[ResultSet] = {
    val byIdF = database.itemsModel.deleteById(item.id)
    val byItemIdF = database.itemsByItemIdsModel.deleteByItemIdAndId(item.itemId, item.id)
    val byBatchIdF = database.itemsByBatchIdsModel.deleteByBatchIdAndId(item.batchId, item.id)

    for {
      byId <- byIdF
      byItemId <- byItemIdF
      byBatchId <- byBatchIdF
    } yield byItemId
  }
}

/**
  * Let available a singleton instance of this service class, to prevent unnecessary instances
  */
object ItemService extends ItemService with ProductionDatabase
object TestItemService extends ItemService with EmbeddedDatabase
object Test3rdPartyItemService extends ItemService with Embedded3rdPartyDatabase
