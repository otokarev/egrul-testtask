package xap.connector

import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import com.websudos.phantom.connectors.{ContactPoint, ContactPoints, KeySpaceDef}

import scala.collection.JavaConversions._

object Connector {
  private val config = ConfigFactory.load()

  private val hosts = config.getStringList("cassandra.host")
  private val inets = hosts.map(InetAddress.getByName)

  private val keyspace: String = config.getString("cassandra.keyspace")

  /**
    * Create a connector with the ability to connects to
    * multiple hosts in a secured cluster
    */
  lazy val connector: KeySpaceDef = ContactPoints(hosts).withClusterBuilder(
    _.withCredentials(
      config.getString("cassandra.username"),
      config.getString("cassandra.password")
    )
  ).keySpace(keyspace)

  /**
    * Create an embedded connector, used for testing purposes
    */
  lazy val testConnector: KeySpaceDef = ContactPoint.embedded.noHeartbeat().keySpace("xap_test")

  lazy val test3rdPartyConnector: KeySpaceDef = ContactPoint.embedded.noHeartbeat().keySpace("xap_3rd_party_test")
}