package xap.service

import com.websudos.phantom.dsl._
import xap.database.DatabaseProvider
import xap.entity.Batch

import scala.concurrent.Future

class BatchService extends DatabaseProvider {

  /**
    * Find batch by Id
    * @param id ItemUpdate's ID that is unique in our database
    * @return
    */
  def get(id: UUID): Future[Option[Batch]] = {
    database.batchesModel.getById(id)
  }

  /**
   * Save an itemUpdate in both tables
   *
   * @param batch Batch
   * @return
   */
  def saveOrUpdate(batch: Batch): Future[ResultSet] = {
      database.batchesModel.store(batch)
  }

}
