package xap.service

import com.websudos.phantom.dsl._
import xap.database.{DatabaseProvider, Embedded3rdPartyDatabase, EmbeddedDatabase, ProductionDatabase}
import xap.entity.Batch

import scala.concurrent.Future

trait BatchService extends DatabaseProvider {

  /**
    * Find batch by Id
    * @param id Item's ID that is unique in our database
    * @return
    */
  def getBatchById(id: UUID): Future[Option[Batch]] = {
    database.batchesModel.getById(id)
  }

  /**
   * Save an item in both tables
   *
   * @param batch Batch
   * @return
   */
  def saveOrUpdate(batch: Batch): Future[ResultSet] = {
      database.batchesModel.store(batch)
  }

}

/**
  * Let available a singleton instance of this service class, to prevent unnecessary instances
  */
object BatchService extends BatchService with ProductionDatabase
object TestBatchService extends BatchService with EmbeddedDatabase
object Test3rdPartyBatchService extends BatchService with Embedded3rdPartyDatabase
