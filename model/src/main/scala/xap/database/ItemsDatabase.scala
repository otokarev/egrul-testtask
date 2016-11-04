package xap.database

import com.websudos.phantom.dsl._
import xap.connector.Connector._
import xap.model.{ConcreteItemsByItemIds, ConcreteItemsModel}


class ItemsDatabase(override val connector: KeySpaceDef) extends Database[ItemsDatabase](connector) {
  object itemsModel extends ConcreteItemsModel with connector.Connector
  object itemsByItemIdsModel extends ConcreteItemsByItemIds with connector.Connector
}


object ProductionDb extends ItemsDatabase(connector)

trait ProductionDatabaseProvider {
  def database: ItemsDatabase
}

trait ProductionDatabase extends ProductionDatabaseProvider {
  override val database = ProductionDb
}


object EmbeddedDb extends ItemsDatabase(testConnector)

trait EmbeddedDatabaseProvider {
  def database: ItemsDatabase
}

trait EmbeddedDatabase extends EmbeddedDatabaseProvider {
  override val database = EmbeddedDb
}