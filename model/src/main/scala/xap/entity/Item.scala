package xap.entity

import com.websudos.phantom.dsl.DateTime

case class Item(
                 id: Long,
                 at: DateTime,
                 payload: String
               )

