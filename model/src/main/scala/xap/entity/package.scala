package xap

import scala.language.implicitConversions

package object entity {

  implicit def impliciteConversionOfItemUpdateToItemUpdateByItemId (itemUpdate: ItemUpdate): ItemUpdateByItemId = {
    ItemUpdateByItemId(itemUpdate.id, itemUpdate.itemId, itemUpdate.modifiedAt)
  }

  implicit def impliciteConversionOfItemUpdateToItemUpdateByBatchId (itemUpdate: ItemUpdate): ItemUpdateByBatchId = {
    ItemUpdateByBatchId(itemUpdate.id, itemUpdate.batchId)
  }

  implicit def impliciteConversionOfBatchWithItemUpdatesToBatch (batchWithItemUpdates: BatchWithItemUpdates): Batch = {
    Batch(batchWithItemUpdates.id, batchWithItemUpdates.createdAt)
  }
}
