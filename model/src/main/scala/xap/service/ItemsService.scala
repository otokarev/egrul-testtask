package xap.service

import com.websudos.phantom.dsl._
import xap.database.ProductionDatabase
import xap.entity.Item

import scala.concurrent.Future

trait ItemsService extends ProductionDatabase {

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
    database.itemsByItemIdsModel.getByItemId(itemId)
  }

  /**
   * Save an item in both tables
   *
   * @param item Item
   * @return
   */
  def saveOrUpdate(item: Item): Future[ResultSet] = {
    for {
      byId <- database.itemsModel.store(item)
      byItemId <- database.itemsByItemIdsModel.store(item)
    } yield byItemId
  }

  /**
   * Delete an item in both tables
   *
   * @param item Item
   * @return
   */
  def delete(item: Item): Future[ResultSet] = {
    for {
      byID <- database.itemsModel.deleteById(item.id)
      byItemId <- database.itemsByItemIdsModel.deleteByItemIdAndId(item.itemId, item.id)
    } yield byItemId
  }
}

/**
  * Let available a singleton instance of this service class, to prevent unnecessary instances
  */
object ItemsService extends ItemsService with ProductionDatabase