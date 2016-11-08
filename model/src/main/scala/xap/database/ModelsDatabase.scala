package xap.database

import com.websudos.phantom.dsl._
import xap.connector.Connector._
import xap.model.{ConcreteBatchesModel, ConcreteItemsByBatchIds, ConcreteItemsByItemIds, ConcreteItemsModel}


class ModelsDatabase(override val connector: KeySpaceDef) extends Database[ModelsDatabase](connector) {
  object itemsModel extends ConcreteItemsModel with connector.Connector
  object batchesModel extends ConcreteBatchesModel with connector.Connector
  object itemsByItemIdsModel extends ConcreteItemsByItemIds with connector.Connector
  object itemsByBatchIdsModel extends ConcreteItemsByBatchIds with connector.Connector
}

object ProductionDb extends ModelsDatabase(connector)

object EmbeddedDb extends ModelsDatabase(testConnector)

object Embedded3rdPartyDb extends ModelsDatabase(test3rdPartyConnector)

trait DatabaseProvider {
  def database: ModelsDatabase
}

trait ProductionDatabase extends DatabaseProvider {
  override val database = ProductionDb
}

trait EmbeddedDatabase extends DatabaseProvider {
  override val database = EmbeddedDb
}

trait Embedded3rdPartyDatabase extends DatabaseProvider {
  override val database = Embedded3rdPartyDb
}
