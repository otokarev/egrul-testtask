package xap.model

import com.websudos.phantom.dsl._
import xap.entity.ItemBase

import scala.concurrent.Future

/**
  * Create the Cassandra representation of the ItemBases table
  */
class ItemBasesModel extends CassandraTable[ConcreteItemBasesModel, ItemBase] {

  override def tableName: String = "itemBases"

  object id extends LongColumn(this) with PartitionKey[Long]
  object createdAt extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Descending

  override def fromRow(r: Row): ItemBase = ItemBase(id(r), createdAt(r))
}

/**
  * Define the available methods for this model
  */
abstract class ConcreteItemBasesModel extends ItemBasesModel with RootConnector {

  def getById(id: Long): Future[Option[ItemBase]] = {
    select
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .one()
  }

  def getByIdList(id: Long): Future[List[ItemBase]] = {
    select
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .fetch()
  }

  def store(itemBase: ItemBase): Future[ResultSet] = {
    insert
      .value(_.id, itemBase.id)
      .value(_.createdAt, itemBase.createdAt)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()
  }

  def deleteById(id: Long): Future[ResultSet] = {
    delete
      .where(_.id eqs id)
      .consistencyLevel_=(ConsistencyLevel.ONE)
      .future()
  }
}
