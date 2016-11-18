package xap.entity

import java.util.UUID

import com.websudos.phantom.dsl.DateTime

case class ItemUpdate(
                 id: UUID,
                 itemId: Long,
                 batchId: Option[UUID],
                 createdAt: DateTime,
                 modifiedAt: DateTime,
                 payload: String
)

case class ItemUpdateByItemId(
                         id: UUID,
                         itemId: Long,
                         createdAt: DateTime
               )
case class ItemUpdateByBatchId(
                         batchId: UUID,
                         id: UUID
                       )
