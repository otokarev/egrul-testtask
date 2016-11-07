package xap

import scala.language.implicitConversions

package object entity {

  implicit def impliciteConversionOfItemToItemByItemId (item: Item): ItemByItemId = {
    ItemByItemId(item.id, item.itemId, item.modifiedAt)
  }

}
