package xap.model

import java.util.UUID

import com.websudos.phantom.dsl._
import xap.entity.Batch

import scala.concurrent.Future

/**
  * Create the Cassandra representation of the Batches table
  */
class BatchesModel extends CassandraTable[ConcreteBatchesModel, Batch] {

  override def tableName: String = "batches"

  object id extends TimeUUIDColumn(this) with PartitionKey[UUID]
  object createdAt extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Descending

  override def fromRow(r: Row): Batch = Batch(id(r), createdAt(r))
}

/**
  * Define the available methods for this model
  */
abstract class ConcreteBatchesModel extends BatchesModel with RootConnector {

  def getById(id: UUID): Future[Option[Batch]] = {
    select
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .one()
  }

  def getByDateTimeRange(range: (DateTime, DateTime)): Future[List[Batch]] = {
    select
      .where(_.createdAt gt range._1)
      .and(_.createdAt lt range._2)
      .allowFiltering()
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .fetch()
  }

  def getByIdList(id: UUID): Future[List[Batch]] = {
    select
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .fetch()
  }

  def store(batch: Batch): Future[ResultSet] = {
    insert
      .value(_.id, batch.id)
      .value(_.createdAt, batch.createdAt)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()
  }

  def deleteById(id: UUID): Future[ResultSet] = {
    delete
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()
  }
}

