package io.lilmonk.model

import java.sql.Timestamp

case class SensorData(sensor_id: Int, temperature: Double, humidity: Double, timestamp: Timestamp)
