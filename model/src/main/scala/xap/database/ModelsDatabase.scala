package xap.database

import com.google.inject.Inject
import com.websudos.phantom.dsl._
import xap.model._


class ModelsDatabase @Inject() (override val connector: KeySpaceDef) extends Database[ModelsDatabase](connector) {
  object itemUpdatesModel extends ConcreteItemUpdatesModel with connector.Connector
  object itemBasesModel extends ConcreteItemBasesModel with connector.Connector
  object batchesModel extends ConcreteBatchesModel with connector.Connector
  object itemUpdatesByItemIdsModel extends ConcreteItemUpdatesByItemIds with connector.Connector
  object itemUpdatesByBatchIdsModel extends ConcreteItemUpdatesByBatchIds with connector.Connector
}

trait DatabaseProvider {
  @Inject val database: ModelsDatabase = null
}

