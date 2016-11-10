package xap.model

import java.util.UUID

import com.websudos.phantom.dsl._
import xap.entity.{ItemUpdate, ItemUpdateByBatchId, ItemUpdateByItemId}

import scala.concurrent.Future

/**
  * Create the Cassandra representation of the ItemUpdates table
  */
class ItemUpdatesModel extends CassandraTable[ConcreteItemUpdatesModel, ItemUpdate] {

  override def tableName: String = "itemUpdates"

  object id extends TimeUUIDColumn(this) with PartitionKey[UUID]
  object itemId extends LongColumn(this)
  object batchId extends OptionalTimeUUIDColumn(this)
  object createdAt extends DateTimeColumn(this)
  object modifiedAt extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Descending
  object payload extends StringColumn(this)

  override def fromRow(r: Row): ItemUpdate = ItemUpdate(id(r), itemId(r), batchId(r), createdAt(r), modifiedAt(r), payload(r))
}

/**
  * Define the available methods for this model
  */
abstract class ConcreteItemUpdatesModel extends ItemUpdatesModel with RootConnector {

  def getById(id: UUID): Future[Option[ItemUpdate]] = {
    select
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .one()
  }

  def getByIdList(id: UUID): Future[List[ItemUpdate]] = {
    select
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .fetch()
  }

  def store(itemUpdate: ItemUpdate): Future[ResultSet] = {
    insert
      .value(_.id, itemUpdate.id)
      .value(_.itemId, itemUpdate.itemId)
      .value(_.batchId, itemUpdate.batchId)
      .value(_.createdAt, itemUpdate.createdAt)
      .value(_.modifiedAt, itemUpdate.modifiedAt)
      .value(_.payload, itemUpdate.payload)
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
/**
  * Create the Cassandra representation of the ItemUpdates by ItemId table
  */
class ItemUpdatesByItemIdModel extends CassandraTable[ConcreteItemUpdatesByItemIds, ItemUpdateByItemId] {

  override def tableName: String = "itemUpdates_by_item_id"

  object itemId extends LongColumn(this) with PartitionKey[Long]
  object id extends TimeUUIDColumn(this) with ClusteringOrder[UUID] with Descending
  object createdAt extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Descending

  override def fromRow(r: Row) = ItemUpdateByItemId(id(r), itemId(r), createdAt(r))
}

/**
  * Define the available methods for this model
  */
abstract class ConcreteItemUpdatesByItemIds extends ItemUpdatesByItemIdModel with RootConnector {

  def getByItemId(itemId: Long): Future[List[ItemUpdateByItemId]] = {
    select
      .where(_.itemId eqs itemId)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .fetch()
  }

  def store(itemUpdate: ItemUpdateByItemId): Future[ResultSet] = {
    insert
      .value(_.id, itemUpdate.id)
      .value(_.itemId, itemUpdate.itemId)
      .value(_.createdAt, itemUpdate.createdAt)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()
  }

  def deleteByItemIdAndId(itemId: Long, id: UUID): Future[ResultSet] = {
    delete
      .where(_.itemId eqs itemId)
      .and(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()
  }
}

/**
  * Create the Cassandra representation of the ItemUpdates by BatchId table
  */
class ItemUpdatesByBatchIdModel extends CassandraTable[ConcreteItemUpdatesByBatchIds, ItemUpdateByBatchId] {

  override def tableName: String = "itemUpdates_by_batch_id"

  object batchId extends UUIDColumn(this) with PartitionKey[UUID]
  object id extends TimeUUIDColumn(this) with PrimaryKey[UUID]

  override def fromRow(r: Row) = ItemUpdateByBatchId(batchId(r), id(r))
}

/**
  * Define the available methods for this model
  */
abstract class ConcreteItemUpdatesByBatchIds extends ItemUpdatesByBatchIdModel with RootConnector {

  def getByBatchId(batchId: UUID): Future[List[ItemUpdateByBatchId]] = {
    select
      .where(_.batchId eqs batchId)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .fetch()
  }

  def store(itemUpdate: ItemUpdateByBatchId): Future[ResultSet] = {
    insert
      .value(_.id, itemUpdate.id)
      .value(_.batchId, itemUpdate.batchId)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()
  }

  def deleteByBatchIdAndId(batchId: UUID, id: UUID): Future[ResultSet] = {
    delete
      .where(_.batchId eqs batchId)
      .and(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()
  }
}
