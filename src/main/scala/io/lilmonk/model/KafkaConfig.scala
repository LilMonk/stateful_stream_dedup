package io.lilmonk.model

case class KafkaConfig(
                        broker: String,
                        sourceTopic: String,
                        sinkTopic: String,
                        startingOffset: String = "earliest",
                        checkpointLocation: String = "/tmp/stateful-dedup-checkpoint",
                        clientId: Option[String] = None,
                        clientSecret: Option[String] = None,
                        securityProtocol: Option[String] = None,
                        saslMechanism: Option[String] = None,
                        sslEndpointIdentificationAlgorithm: Option[String] = None,
                        processingTriggerTime: String = "2 minutes"
                      ) {
  // Generates the JAAS configuration if all required parameters are provided
  val jaasConfig: Option[String] = for {
    protocol <- securityProtocol
    mechanism <- saslMechanism
    client <- clientId
    secret <- clientSecret
    loginModule <- getLoginModule(protocol, mechanism)
  } yield s"""$loginModule required username="$client" password="$secret";"""

  // Maps security protocol and SASL mechanism to the appropriate login module
  private def getLoginModule(protocol: String, mechanism: String): Option[String] = {
    (protocol, mechanism) match {
      case ("SASL_SSL", "SCRAM-SHA-256") | ("SASL_PLAINTEXT", "SCRAM-SHA-256") =>
        Some("org.apache.kafka.common.security.scram.ScramLoginModule")
      case ("SASL_SSL", "SCRAM-SHA-512") | ("SASL_PLAINTEXT", "SCRAM-SHA-512") =>
        Some("org.apache.kafka.common.security.scram.ScramLoginModule")
      case ("SASL_PLAINTEXT", "PLAIN") =>
        Some("org.apache.kafka.common.security.plain.PlainLoginModule")
      case _ =>
        None
    }
  }
}
