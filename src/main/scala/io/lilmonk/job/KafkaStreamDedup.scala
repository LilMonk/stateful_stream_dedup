package io.lilmonk.job

import io.lilmonk.model.{KafkaConfig, SensorData}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, from_json}
import org.apache.spark.sql.streaming.{GroupStateTimeout, OutputMode, Trigger}
import org.apache.spark.sql.types._

import java.sql.Timestamp

// Case class for state
case class DeduplicationState(latestTimestamp: Timestamp)


object KafkaStreamDedup {
  private val SENSOR_DATA_SCHEMA = StructType(Array(
    StructField("sensor_id", IntegerType, nullable = false),
    StructField("temperature", DoubleType, nullable = true),
    StructField("humidity", FloatType, nullable = true),
    StructField("timestamp", TimestampType, nullable = false)
  ))
  private val DEFAULT_TIMESTAMP = Timestamp.valueOf("1970-01-01 00:00:00")

  def run(spark: SparkSession, config: KafkaConfig): Unit = {
    import spark.implicits._
    val inputStream = if (config.jaasConfig.isDefined) {
      spark.readStream.format("kafka")
        .option("kafka.bootstrap.servers", config.broker)
        .option("kafka.security.protocol", config.securityProtocol.getOrElse("SASL_SSL"))
        .option("kafka.sasl.mechanism", config.saslMechanism.getOrElse("PLAIN"))
        .option("kafka.sasl.jaas.config", config.jaasConfig.getOrElse(""))
        .option("kafka.ssl.endpoint.identification.algorithm", config.sslEndpointIdentificationAlgorithm.getOrElse("https"))
        .option("subscribe", config.sourceTopic)
        .option("startingOffsets", config.startingOffset)
        .load()
    }
    else {
      spark.readStream.format("kafka")
        .option("kafka.bootstrap.servers", config.broker)
        .option("subscribe", config.sourceTopic)
        .option("startingOffsets", config.startingOffset)
        .load()
    }

    val parsedStream = inputStream.selectExpr("CAST(value AS STRING) as json")
      .select(from_json(col("json"), SENSOR_DATA_SCHEMA).as("data"))
      .select("data.*")
      .as[SensorData]

    val deduplicatedStream = parsedStream
      .dropDuplicates("sensor_id", "timestamp")
      .groupByKey(record => record.sensor_id)
      .flatMapGroupsWithState[DeduplicationState, SensorData](OutputMode.Append(), GroupStateTimeout.NoTimeout()) {
        case (sensor_id, records, state) =>
          // Get the latest timestamp from state
          val latestTimestamp = state.getOption.map(_.latestTimestamp).getOrElse(DEFAULT_TIMESTAMP)

          // Sort records by timestamp and filter out duplicates
          val deduplicatedRecords = records
            .filter(record => record.timestamp.after(latestTimestamp))
            .toSeq
            .sortBy(_.timestamp)

          // Update the state with the latest timestamp
          if (deduplicatedRecords.nonEmpty) {
            val newLatestTimestamp = deduplicatedRecords.map(_.timestamp).max(Ordering[Timestamp])
            state.update(DeduplicationState(newLatestTimestamp))
          }

          deduplicatedRecords.iterator
      }

    if (config.jaasConfig.isDefined) {
      deduplicatedStream
        .selectExpr("CAST(sensor_id AS STRING) AS key", "to_json(struct(*)) AS value")
        .writeStream
        .outputMode("append")
        .format("kafka")
        .option("kafka.bootstrap.servers", config.broker)
        .option("kafka.security.protocol", config.securityProtocol.getOrElse("SASL_SSL"))
        .option("kafka.sasl.mechanism", config.saslMechanism.getOrElse("PLAIN"))
        .option("kafka.sasl.jaas.config", config.jaasConfig.getOrElse(""))
        .option("kafka.ssl.endpoint.identification.algorithm", config.sslEndpointIdentificationAlgorithm.getOrElse("https"))
        .option("topic", config.sinkTopic)
        .option("checkpointLocation", config.checkpointLocation)
        .trigger(Trigger.ProcessingTime(config.processingTriggerTime))
        .start()
        .awaitTermination()
    }
    else {
      deduplicatedStream
        .selectExpr("CAST(sensor_id AS STRING) AS key", "to_json(struct(*)) AS value")
        .writeStream
        .outputMode("append")
        .format("kafka")
        .option("kafka.bootstrap.servers", config.broker)
        .option("topic", config.sinkTopic)
        .option("checkpointLocation", config.checkpointLocation)
        .trigger(Trigger.ProcessingTime(config.processingTriggerTime))
        .start()
        .awaitTermination()
    }
  }

}
