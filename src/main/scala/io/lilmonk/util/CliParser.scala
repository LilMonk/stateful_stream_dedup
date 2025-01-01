package io.lilmonk.util

import scopt.OParser

object Env extends Enumeration {
  type Env = Value
  val Local = Value("local")
  val Dev = Value("dev")
  val Prod = Value("prod")
}

object JobType extends Enumeration {
  type JobType = Value
  val KafkaSamplesIngestion = Value("KAFKA_SAMPLE_INGESTION")
  val KafkaStreamDedup = Value("KAFKA_STREAM_DEDUP")
}

case class CommandLineConfig(
                              env: Option[Env.Value] = None,
                              jobType: Option[JobType.Value] = None,
                              configFile: Option[String] = None
                            )

object CliParser {
  def parse(args: Array[String]): Option[CommandLineConfig] = {
    val builder = OParser.builder[CommandLineConfig]
    val parser = {
      import builder._
      OParser.sequence(
        programName("JobRunner"),
        head("JobRunner", "1.0"),
        opt[String]('e', "env")
          .required()
          .action((x, c) => c.copy(env = Env.values.find(_.toString == x)))
          .validate(x =>
            if (Env.values.exists(_.toString == x)) success
            else failure(s"Invalid environment: $x. Valid values are: ${Env.values.mkString(", ")}.")
          )
          .text(s"Environment to run the job. Valid values are: ${Env.values.mkString(", ")}."),
        opt[String]('j', "jobType")
          .required()
          .action((x, c) => c.copy(jobType = JobType.values.find(_.toString == x)))
          .validate(x =>
            if (JobType.values.exists(_.toString == x)) success
            else failure(s"Invalid job type: $x. Valid values are: ${JobType.values.mkString(", ")}.")
          )
          .text(s"Job type to run. Valid values are: ${JobType.values.mkString(", ")}."),
        opt[String]('c', "config")
          .required()
          .valueName("<file>")
          .action((x, c) => c.copy(configFile = Some(x)))
          .validate(x =>
            if (x.endsWith(".conf") || x.endsWith(".properties")) success
            else failure(s"Invalid config file: $x. Config file must be a .conf or .properties file.")
          )
          .text("Path to the .conf or .properties config file"),
      )
    }

    // Parse the arguments
    OParser.parse(parser, args, CommandLineConfig())
  }
}

