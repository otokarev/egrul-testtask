package xap.test.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.datastax.driver.core.Session
import com.google.inject.{Guice, Injector}
import com.websudos.phantom.connectors.KeySpace
import com.websudos.phantom.dsl.KeySpaceDef
import xap.database.ModelsDatabase

trait WithGuiceInjectorAndImplicites {
  val injector: Injector = Guice.createInjector(new Module)
  val database: ModelsDatabase = injector.getInstance(classOf[ModelsDatabase])
  val connector: KeySpaceDef = injector.getInstance(classOf[KeySpaceDef])
  implicit val keySpace = KeySpace(connector.name)
  implicit val session: Session = connector.session

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()


}
