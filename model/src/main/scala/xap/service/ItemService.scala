package xap.service

import com.datastax.driver.core.utils.UUIDs
import com.websudos.phantom.dsl._
import org.joda.time.DateTime
import xap.database.{DatabaseProvider, Embedded3rdPartyDatabase, EmbeddedDatabase, ProductionDatabase}
import xap.entity.{Item, ItemBase, ItemUpdate}

import scala.concurrent.Future

trait ItemService extends DatabaseProvider {

  /**
    * Find item by Id
    * @param id ItemBase's ID
    * @return
    */
  def getById(id: Long): Future[Option[Item]] = {
    val itemBaseF = database.itemBasesModel.getById(id)

    val maybeItemUpdateF: Future[Option[ItemUpdate]] = for {
      maybeItemBase <- itemBaseF if maybeItemBase.isDefined
      // TODO: should return last update
      itemUpdateByItemIdList <- database.itemUpdatesByItemIdsModel.getByItemId(maybeItemBase.get.id)
      maybeItemUpdate <- database.itemUpdatesModel.getById(itemUpdateByItemIdList.last.id)
    } yield maybeItemUpdate

    maybeItemUpdateF map {
      case Some(a) => Some(Item(a.itemId, Some(a.createdAt), a.payload))
      case _ => None
    }

  }

  /**
    * Save an itemUpdate in both tables
    *
    * @param item Item
    * @return
    */
  def saveOrUpdate(item: Item): Future[ResultSet] = {

    database.itemBasesModel.getById(item.id).flatMap {
      case Some(itemBase) =>
        assert(itemBase != item.asInstanceOf[ItemBase], "Stored ItemBase cannot be changed")
        val itemUpdate = ItemUpdate(
          UUIDs.timeBased(),
          itemBase.id,
          None,
          itemBase.createdAt,
          DateTime.now(),
          item.payload)

        database.itemUpdatesModel.store(itemUpdate)
      case None =>
        // TODO: who catches failure if there are no onFailure?
        val now = DateTime.now()
        val itemBaseF = database.itemBasesModel.store(ItemBase(item.id, now))
        // TODO: Store ItemUpdate
        val itemUpdateF = database.itemUpdatesModel.store(ItemUpdate(
          UUIDs.timeBased(),
          item.id,
          None,
          now,
          now,
          item.payload))
        for {
          itemBase <- itemBaseF
          itemUpdate <- itemUpdateF
          // TODO: What does ResultSet contain for `insert`???
        } yield itemBase
    }

  }

}

/**
  * Let available a singleton instance of this service class, to prevent unnecessary instances
  */
object ItemService extends ItemBaseService with ProductionDatabase
object TestItemService extends ItemBaseService with EmbeddedDatabase
object Test3rdPartyBaseService extends ItemBaseService with Embedded3rdPartyDatabase

