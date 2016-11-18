package xap

import scala.language.implicitConversions

package object entity {

  implicit def impliciteConversionOfItemUpdateToItemUpdateByItemId (itemUpdate: ItemUpdate): ItemUpdateByItemId = {
    ItemUpdateByItemId(itemUpdate.id, itemUpdate.itemId, itemUpdate.modifiedAt)
  }

  implicit def impliciteConversionOfItemUpdateToItemUpdateByBatchId (itemUpdate: ItemUpdate): ItemUpdateByBatchId = {
    ItemUpdateByBatchId(itemUpdate.batchId.get, itemUpdate.id)
  }

  implicit def impliciteConversionOfBatchWithItemUpdatesToBatch (batchWithItemUpdates: BatchWithItemUpdates): Batch = {
    Batch(batchWithItemUpdates.id, batchWithItemUpdates.createdAt)
  }

  implicit def impliciteConversionOfItemToItemBase (item: Item): ItemBase = {
    ItemBase(item.id, item.at)
  }
}
