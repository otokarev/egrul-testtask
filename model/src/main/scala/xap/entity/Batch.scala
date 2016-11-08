package xap.entity

import java.util.UUID

import com.websudos.phantom.dsl.DateTime

case class Batch(
                 id: UUID,
                 createdAt: DateTime
               )
case class BatchWithItems(
                  id: UUID,
                  createdAt: DateTime,
                  items: List[Item]
                )

