package xap.service

import com.websudos.phantom.dsl._
import xap.database.DatabaseProvider
import xap.entity.ItemBase

import scala.concurrent.Future

class ItemBaseService extends DatabaseProvider {

  /**
    * Find itemBase by Id
    * @param id ItemBase's ID that is unique in our database
    * @return
    */
  def getById(id: Long): Future[Option[ItemBase]] = {
    database.itemBasesModel.getById(id)
  }

  /**
    * Save an itemUpdate in both tables
    *
    * @param itemBase ItemBase object
    * @return
    */
  def saveOrUpdate(itemBase: ItemBase): Future[ResultSet] = {
    database.itemBasesModel.store(itemBase)
  }

}
