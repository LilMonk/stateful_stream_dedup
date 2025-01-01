package io.lilmonk

import com.typesafe.config.{Config, ConfigFactory}
import io.lilmonk.model.KafkaConfig
import io.lilmonk.util.{CliParser, Env, JobType}
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

import scala.reflect.io.File
import scala.util.Try


object JobRunner {
  private def getSpark(isLocalRun: Boolean, config: Config) : SparkSession = {
    if (isLocalRun) {
      SparkSession.builder()
        .master("local[*]")
        .appName("KafkaStreamDedup")
        .config("spark.driver.extraJavaOptions", "-Duser.timezone=GMT")
        .config("spark.executor.extraJavaOptions", "-Duser.timezone=GMT")
        .config("spark.sql.session.timeZone", "UTC")
        .config("spark.sql.warehouse.dir", config.getString("spark.sql_warehouse_dir"))
        .getOrCreate()
    }else{
      SparkSession.builder()
        .appName("KafkaStreamDedup")
        .config("spark.driver.extraJavaOptions", "-Duser.timezone=GMT")
        .config("spark.driver.extraJavaOptions", "-Duser.timezone=GMT")
        .config("spark.executor.extraJavaOptions", "-Duser.timezone=GMT")
        .config("spark.sql.session.timeZone", "UTC")
        .config("spark.sql.warehouse.dir", config.getString("spark.sql_warehouse_dir"))
        .getOrCreate()
    }
  }

  private def runKafkaStreamDedup(spark: SparkSession, kafkaConfig: KafkaConfig): Unit = {
    val kafkaStreamDedup = job.KafkaStreamDedup
    kafkaStreamDedup.run(spark, kafkaConfig)
    spark.stop()
  }

  private def runKafkaSampleIngestion(config: KafkaConfig): Unit = {
    val kafkaSampleIngestion = job.KafkaSamplesIngestion
    kafkaSampleIngestion.run(config)
  }

  def main(args: Array[String]): Unit = {
    val logger = Logger.getLogger(this.getClass)

    val cliConfig = CliParser.parse(args)
    if (cliConfig.isDefined) {
      val commandLineConfig = cliConfig.get
      val env = commandLineConfig.env.get
      val configFile = commandLineConfig.configFile.get
      val config = ConfigFactory.parseFile(File(configFile).jfile).resolve()
      logger.info(s"Running the job in ${env.toString} environment")
      val kafkaConfig = KafkaConfig(
        broker = config.getString("kafka.broker"),
        sourceTopic = config.getString("kafka.source_topic"),
        sinkTopic = config.getString("kafka.sink_topic"),
        startingOffset = config.getString("kafka.starting_offset"),
        checkpointLocation = config.getString("kafka.checkpoint_location"),
        clientId = Try(config.getString("kafka.client_id")).toOption,
        clientSecret = Try(config.getString("kafka.client_secret")).toOption,
        saslMechanism = Try(config.getString("kafka.sasl_mechanism")).toOption,
        securityProtocol = Try(config.getString("kafka.security_protocol")).toOption,
        sslEndpointIdentificationAlgorithm = Try(config.getString("kafka.ssl_endpoint_identification_algorithm")).toOption,
        processingTriggerTime = config.getString("kafka.processing_trigger_time"),
      )
      commandLineConfig.jobType match {
        case Some(JobType.KafkaStreamDedup) => {
          val spark = getSpark(env == Env.Local, config)
          runKafkaStreamDedup(spark, kafkaConfig)
        }
        case Some(JobType.KafkaSamplesIngestion) => {
          runKafkaSampleIngestion(kafkaConfig)
        }
        case _ => logger.error("Invalid job type. Valid values are: KafkaStreamDedup")
      }
    }
    else {
      logger.error("Invalid command line arguments. Please check the usage.")
    }
  }
}