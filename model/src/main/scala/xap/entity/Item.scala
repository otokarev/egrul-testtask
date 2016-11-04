package xap.entity

import java.util.UUID

import com.websudos.phantom.dsl.DateTime

case class Item(
  id: UUID,
  itemId: Long,
  creationDate: DateTime,
  payload: String
)