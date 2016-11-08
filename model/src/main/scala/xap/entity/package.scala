package xap

import scala.language.implicitConversions

package object entity {

  implicit def impliciteConversionOfItemToItemByItemId (item: Item): ItemByItemId = {
    ItemByItemId(item.id, item.itemId, item.modifiedAt)
  }

  implicit def impliciteConversionOfItemToItemByBatchId (item: Item): ItemByBatchId = {
    ItemByBatchId(item.id, item.batchId)
  }

  implicit def impliciteConversionOfBatchWithItemsToBatch (batchWithItems: BatchWithItems): Batch = {
    Batch(batchWithItems.id, batchWithItems.createdAt)
  }
}
