package xap.service

import java.util.NoSuchElementException

import com.datastax.driver.core.utils.UUIDs
import com.google.inject.Inject
import com.websudos.phantom.dsl._
import xap.database.DatabaseProvider
import xap.entity._

import scala.concurrent.Future

class ItemService @Inject() (ItemUpdateService: ItemUpdateService) extends DatabaseProvider {

  /**
   * Find item by Id
   * @param id ItemBase's ID
   * @return
   */
  def getById(id: Long): Future[Option[Item]] = {
    val itemBaseF = database.itemBasesModel.getById(id)

    val maybeItemUpdateF: Future[Option[ItemUpdate]] = for {
      maybeItemBase <- itemBaseF if maybeItemBase.isDefined
      maybeItemUpdate <- ItemUpdateService.getLastForItemId(maybeItemBase.get.id)
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
          item.payload
        )

        ItemUpdateService.saveOrUpdate(itemUpdate)
      case None =>
        // TODO: who catches failure if there are no onFailure?
        val now = item.at
        val itemBaseF = database.itemBasesModel.store(ItemBase(item.id, now))
        val itemUpdateF = ItemUpdateService.saveOrUpdate(
          ItemUpdate(
            UUIDs.timeBased(),
            item.id,
            None,
            now,
            now,
            item.payload
          )
        )
        for {
          itemBase <- itemBaseF
          itemUpdate <- itemUpdateF
        } yield itemBase
    }

  }

}
