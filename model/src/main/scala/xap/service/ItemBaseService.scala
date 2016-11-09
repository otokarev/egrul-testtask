package xap.service

import com.websudos.phantom.dsl._
import xap.database.{DatabaseProvider, Embedded3rdPartyDatabase, EmbeddedDatabase, ProductionDatabase}
import xap.entity.ItemBase

import scala.concurrent.Future

trait ItemBaseService extends DatabaseProvider {

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

/**
  * Let available a singleton instance of this service class, to prevent unnecessary instances
  */
object ItemBaseService extends ItemBaseService with ProductionDatabase
object TestItemBaseService extends ItemBaseService with EmbeddedDatabase
object Test3rdPartyItemBaseService extends ItemBaseService with Embedded3rdPartyDatabase

