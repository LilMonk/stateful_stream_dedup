package io.lilmonk.job

import io.lilmonk.model.{KafkaConfig, SensorData}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.log4j.Logger
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints}

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.Properties
import scala.util.Random


object KafkaSamplesIngestion {
  val logger: Logger = Logger.getLogger(this.getClass)

  def run( config: KafkaConfig): Unit = {
    val props = new Properties()
    props.put("bootstrap.servers", config.broker)

    if (config.securityProtocol.isDefined && config.saslMechanism.isDefined && config.jaasConfig.isDefined) {
      props.put("security.protocol", config.securityProtocol.get)
      props.put("sasl.mechanism", config.saslMechanism.get)
      props.put("sasl.jaas.config", config.jaasConfig.get)
    }

    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)

    def generateSensorData(timestamp: Timestamp): SensorData = {
      SensorData(
        Random.nextInt(10) + 1,
        BigDecimal(Random.nextDouble() * 10 + 20).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
        BigDecimal(Random.nextDouble() * 20 + 30).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
        timestamp
      )
    }

    def generateEvents(): Unit = {
      var lastTimestamp = LocalDateTime.now()
      while (true) {
        var timestamp = LocalDateTime.now()
        if (Random.nextBoolean()) {
          timestamp = lastTimestamp.minusSeconds(Random.nextInt(10) + 1)
        } else if (Random.nextBoolean()) {
          timestamp = lastTimestamp
        } else {
          lastTimestamp = LocalDateTime.now()
          timestamp = lastTimestamp
        }
        val event = generateSensorData(Timestamp.valueOf(timestamp))

        try {
          implicit val formats: AnyRef with Formats = Serialization.formats(NoTypeHints)
          val jsonPayload = Serialization.write(event)
          val key = event.sensor_id.toString // Use sensor_id as the key for partitioning
          val record = new ProducerRecord[String, String](config.sourceTopic, key, jsonPayload)
          producer.send(record)
          logger.info(s"Sent event: $jsonPayload")
          Thread.sleep(1000)
        } catch {
          case e: Exception =>
            throw new Exception(s"Failed to send event: $event, error: $e")
        }
      }
    }

    generateEvents()
  }
}