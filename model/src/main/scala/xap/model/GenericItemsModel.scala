package xap.model

import java.util.UUID

import com.websudos.phantom.dsl._
import xap.entity.{Item, ItemByItemId}

import scala.concurrent.Future

/**
  * Create the Cassandra representation of the Items table
  */
class ItemsModel extends CassandraTable[ConcreteItemsModel, Item] {

  override def tableName: String = "items"

  object id extends TimeUUIDColumn(this) with PartitionKey[UUID]
  object itemId extends LongColumn(this) with ClusteringOrder[Long] with Descending
  object createdAt extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Descending
  object modifiedAt extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Descending
  object payload extends StringColumn(this)

  override def fromRow(r: Row): Item = Item(id(r), itemId(r), createdAt(r), modifiedAt(r), payload(r))
}

/**
  * Define the available methods for this model
  */
abstract class ConcreteItemsModel extends ItemsModel with RootConnector {

  def getById(id: UUID): Future[Option[Item]] = {
    select
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .one()
  }

  def getByIdList(id: UUID): Future[List[Item]] = {
    select
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .fetch()
  }

  def store(item: Item): Future[ResultSet] = {
    insert
      .value(_.id, item.id)
      .value(_.itemId, item.itemId)
      .value(_.createdAt, item.createdAt)
      .value(_.modifiedAt, item.modifiedAt)
      .value(_.payload, item.payload)
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
  * Create the Cassandra representation of the Items by ItemId table
  */
class ItemsByItemIdModel extends CassandraTable[ConcreteItemsByItemIds, ItemByItemId] {

  override def tableName: String = "items_by_item_id"

  object itemId extends LongColumn(this) with PartitionKey[Long]
  object id extends TimeUUIDColumn(this) with ClusteringOrder[UUID] with Descending
  object createdAt extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Descending

  override def fromRow(r: Row) = ItemByItemId(id(r), itemId(r), createdAt(r))
}

/**
  * Define the available methods for this model
  */
abstract class ConcreteItemsByItemIds extends ItemsByItemIdModel with RootConnector {

  def getByItemId(itemId: Long): Future[List[ItemByItemId]] = {
    select
      .where(_.itemId eqs itemId)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .fetch()
  }

  def store(item: ItemByItemId): Future[ResultSet] = {
    insert
      .value(_.id, item.id)
      .value(_.itemId, item.itemId)
      .value(_.createdAt, item.createdAt)
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