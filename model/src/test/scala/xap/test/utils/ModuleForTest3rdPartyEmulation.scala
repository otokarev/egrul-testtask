package xap.test.utils

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.websudos.phantom.connectors.KeySpaceDef
import xap.connector.Connector

class ModuleForTest3rdPartyEmulation extends AbstractModule{
  @Override
  protected def configure(): Unit = {

    @Provides
    @Singleton
    def dbConnection: KeySpaceDef = Connector.test3rdPartyConnector
  }

}
