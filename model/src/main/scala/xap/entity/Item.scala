package xap.entity

import java.util.UUID

import com.websudos.phantom.dsl.DateTime

case class Item(
                 id: UUID,
                 itemId: Long,
                 batchId: UUID,
                 createdAt: DateTime,
                 modifiedAt: DateTime,
                 payload: String
)

case class ItemByItemId(
                         id: UUID,
                         itemId: Long,
                         createdAt: DateTime
               )
case class ItemByBatchId(
                         id: UUID,
                         batchId: UUID
                       )
