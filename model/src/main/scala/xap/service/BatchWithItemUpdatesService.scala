package xap.service

import javax.inject.Inject

import com.websudos.phantom.dsl._
import xap.database.DatabaseProvider
import xap.entity.BatchWithItemUpdates

import scala.concurrent.Future

class BatchWithItemUpdatesService @Inject()(BatchService: BatchService, ItemUpdateService: ItemUpdateService) extends DatabaseProvider {

  /**
    * Find batch by Id
    * @param id ItemUpdate's ID that is unique in our database
    * @return
    */
  def get(id: UUID): Future[Option[BatchWithItemUpdates]] = {
    val batchF = database.batchesModel.getById(id)
    val batchedItemUpdatesF = database.itemUpdatesByBatchIdsModel.getByBatchId(id)

    val f = for {
      batchedItemUpdates <- batchedItemUpdatesF
      itemUpdates <- Future.traverse(batchedItemUpdates)(a => database.itemUpdatesModel.getByIdList(a.id))
      batch <- batchF
    } yield (itemUpdates.flatten, batch)

    f map {
      case Tuple2(itemUpdates, Some(batch)) => Option(BatchWithItemUpdates(batch.id, batch.createdAt, itemUpdates))
      case _ => None
    }
  }

  /**
    * Find batch with item updates by DateTime range
    * @param range (DateTime, DateTime)
    * @return
    */
  def getByDateTimeRange(range: (DateTime, DateTime)): Future[List[BatchWithItemUpdates]] = {
    val r = BatchService.getByDateTimeRange(range)
      .flatMap(
        l => Future.sequence(
          l.map {b => get(b.id).collect {case Some(a) => a} }
        )
      )
    r
  }

  /**
    * Save an itemUpdate in both tables
    *
    * @param batchWithItemUpdates BatchWithItemUpdates
    * @return
    */
  def saveOrUpdate(batchWithItemUpdates: BatchWithItemUpdates): Future[ResultSet] = {
    val itemUpdatesF = Future.sequence(batchWithItemUpdates.itemUpdates.map {i =>
      ItemUpdateService.saveOrUpdate(i.copy(batchId = Some(batchWithItemUpdates.id)))
    })

    val batchF = BatchService.saveOrUpdate(batchWithItemUpdates)

    for {
      itemUpdates <- itemUpdatesF
      batch <- batchF
    } yield batch
  }

}

