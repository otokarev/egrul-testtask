package xap.test.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Guice
import com.websudos.phantom.connectors.KeySpace
import com.websudos.phantom.dsl.KeySpaceDef
import xap.database.ModelsDatabase

trait WithGuiceInjectorAndImplicites {
  val injector = Guice.createInjector(new Module)
  val database = injector.getInstance(classOf[ModelsDatabase])
  val connector: KeySpaceDef = injector.getInstance(classOf[KeySpaceDef])
  implicit val session = connector.session
  implicit val keySpace = KeySpace(connector.name)

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()


}
