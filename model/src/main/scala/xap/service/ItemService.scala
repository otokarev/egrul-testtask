package xap.service

import java.util.NoSuchElementException

import com.datastax.driver.core.utils.UUIDs
import com.websudos.phantom.dsl._
import xap.database.DatabaseProvider
import xap.entity._

import scala.concurrent.Future

class ItemService extends DatabaseProvider {

  /**
    * Find item by Id
    * @param id ItemBase's ID
    * @return
    */
  def getById(id: Long): Future[Option[Item]] = {
    val itemBaseF = database.itemBasesModel.getById(id)

    val maybeItemUpdateF: Future[Option[ItemUpdate]] = for {
      maybeItemBase <- itemBaseF if maybeItemBase.isDefined
      itemUpdateByItemIdList <- database.itemUpdatesByItemIdsModel.getByItemId(maybeItemBase.get.id)
      maybeItemUpdate <- database.itemUpdatesModel.getById(itemUpdateByItemIdList.head.id)
    } yield maybeItemUpdate

    maybeItemUpdateF recover {
      case e: NoSuchElementException => None
    } map {
      case Some(a) => Some(Item(a.itemId, a.modifiedAt, a.payload))
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
        //noinspection ComparingUnrelatedTypes
        assert(itemBase != item, "Stored ItemBase cannot be changed")
        val itemUpdate = ItemUpdate(
          UUIDs.timeBased(),
          itemBase.id,
          None,
          itemBase.createdAt,
          item.at,
          item.payload)

        val itemUpdateF = database.itemUpdatesModel.store(itemUpdate)
        val itemUpdateByItemIdF = database.itemUpdatesByItemIdsModel.store(itemUpdate)
        for {
          itemUpdate <- itemUpdateF
          itemUpdateByItemId <- itemUpdateByItemIdF
        } yield itemUpdate
      case None =>
        // TODO: who catches failure if there are no onFailure?
        val now = item.at
        val itemBaseF = database.itemBasesModel.store(ItemBase(item.id, now))
        // TODO: Refactoring: whole ItemUpdate internals must be encapsulated in ItemUpdateService
        val itemUpdate = ItemUpdate(
          UUIDs.timeBased(),
          item.id,
          None,
          now,
          now,
          item.payload)
        val itemUpdateF = database.itemUpdatesModel.store(itemUpdate)
        val itemUpdateByItemIdF = database.itemUpdatesByItemIdsModel.store(itemUpdate)
        for {
          itemBase <- itemBaseF
          itemUpdate <- itemUpdateF
          itemUpdateByItemId <- itemUpdateByItemIdF
          // TODO: What does ResultSet contain for `insert`???
        } yield itemBase
    }

  }

}
