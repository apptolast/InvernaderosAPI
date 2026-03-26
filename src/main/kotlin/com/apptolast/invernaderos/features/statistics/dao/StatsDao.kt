package com.apptolast.invernaderos.features.statistics.dao

import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsDailyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsHourlyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsMonthlyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsWeeklyDto
import java.sql.ResultSet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

/**
 * DAO para consultas de estadisticas agregadas en TimescaleDB.
 *
 * Usa JDBC nativo para queries a continuous aggregates:
 * - iot.readings_hourly  (agregaciones horarias por code)  -> DAY
 * - iot.readings_daily   (agregaciones diarias por code)   -> WEEK, MONTH
 * - iot.readings_weekly  (agregaciones semanales por code)  -> YEAR
 * - iot.readings_monthly (agregaciones mensuales por code)  -> ALL
 *
 * PRINCIPIO ARQUITECTONICO:
 * TimescaleDB solo tiene (time, code, value). Los metadatos de negocio
 * (greenhouse_id, sensor_type, unit) se resuelven desde PostgreSQL
 * en la capa de servicio. El code es la clave de cruce.
 */
class StatsDao(@Qualifier("timescaleJdbcTemplate") private val jdbcTemplate: JdbcTemplate) {

    private val hourlyRowMapper = RowMapper { rs: ResultSet, _: Int ->
        SensorStatisticsHourlyDto(
            bucket = rs.getTimestamp("bucket").toInstant(),
            code = rs.getString("code"),
            avgValue = rs.getDouble("avg_value").takeUnless { rs.wasNull() },
            minValue = rs.getDouble("min_value").takeUnless { rs.wasNull() },
            maxValue = rs.getDouble("max_value").takeUnless { rs.wasNull() },
            stddevValue = rs.getDouble("stddev_value").takeUnless { rs.wasNull() },
            countReadings = rs.getLong("count_readings")
        )
    }

    private val dailyRowMapper = RowMapper { rs: ResultSet, _: Int ->
        SensorStatisticsDailyDto(
            bucket = rs.getTimestamp("bucket").toInstant(),
            code = rs.getString("code"),
            avgValue = rs.getDouble("avg_value").takeUnless { rs.wasNull() },
            minValue = rs.getDouble("min_value").takeUnless { rs.wasNull() },
            maxValue = rs.getDouble("max_value").takeUnless { rs.wasNull() },
            stddevValue = rs.getDouble("stddev_value").takeUnless { rs.wasNull() },
            countReadings = rs.getLong("count_readings")
        )
    }

    private val weeklyRowMapper = RowMapper { rs: ResultSet, _: Int ->
        SensorStatisticsWeeklyDto(
            bucket = rs.getTimestamp("bucket").toInstant(),
            code = rs.getString("code"),
            avgValue = rs.getDouble("avg_value").takeUnless { rs.wasNull() },
            minValue = rs.getDouble("min_value").takeUnless { rs.wasNull() },
            maxValue = rs.getDouble("max_value").takeUnless { rs.wasNull() },
            stddevValue = rs.getDouble("stddev_value").takeUnless { rs.wasNull() },
            countReadings = rs.getLong("count_readings")
        )
    }

    private val monthlyRowMapper = RowMapper { rs: ResultSet, _: Int ->
        SensorStatisticsMonthlyDto(
            bucket = rs.getTimestamp("bucket").toInstant(),
            code = rs.getString("code"),
            avgValue = rs.getDouble("avg_value").takeUnless { rs.wasNull() },
            minValue = rs.getDouble("min_value").takeUnless { rs.wasNull() },
            maxValue = rs.getDouble("max_value").takeUnless { rs.wasNull() },
            stddevValue = rs.getDouble("stddev_value").takeUnless { rs.wasNull() },
            countReadings = rs.getLong("count_readings")
        )
    }

    /**
     * Estadisticas horarias para un code en las ultimas N horas.
     * Lee de iot.readings_hourly (continuous aggregate).
     */
    fun getHourlyStatistics(code: String, hours: Int = 24): List<SensorStatisticsHourlyDto> {
        val sql = """
            SELECT bucket, code, avg_value, min_value, max_value, stddev_value, count_readings
            FROM iot.readings_hourly
            WHERE code = ?
              AND bucket >= NOW() - (? * INTERVAL '1 hour')
            ORDER BY bucket ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, hourlyRowMapper, code, hours)
    }

    /**
     * Estadisticas diarias para un code en los ultimos N dias.
     * Lee de iot.readings_daily (continuous aggregate).
     */
    fun getDailyStatistics(code: String, days: Int = 7): List<SensorStatisticsDailyDto> {
        val sql = """
            SELECT bucket, code, avg_value, min_value, max_value, stddev_value, count_readings
            FROM iot.readings_daily
            WHERE code = ?
              AND bucket >= NOW() - (? * INTERVAL '1 day')
            ORDER BY bucket ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, dailyRowMapper, code, days)
    }

    /**
     * Estadisticas semanales para un code en las ultimas N semanas.
     * Lee de iot.readings_weekly (continuous aggregate).
     * Usado para la escala YEAR (~52 puntos).
     */
    fun getWeeklyStatistics(code: String, weeks: Int = 52): List<SensorStatisticsWeeklyDto> {
        val sql = """
            SELECT bucket, code, avg_value, min_value, max_value, stddev_value, count_readings
            FROM iot.readings_weekly
            WHERE code = ?
              AND bucket >= NOW() - (? * INTERVAL '1 week')
            ORDER BY bucket ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, weeklyRowMapper, code, weeks)
    }

    /**
     * Estadisticas mensuales para un code en los ultimos N meses.
     * Lee de iot.readings_monthly (continuous aggregate).
     * Usado para la escala ALL (todos los datos historicos).
     */
    fun getMonthlyStatistics(code: String, months: Int = 120): List<SensorStatisticsMonthlyDto> {
        val sql = """
            SELECT bucket, code, avg_value, min_value, max_value, stddev_value, count_readings
            FROM iot.readings_monthly
            WHERE code = ?
              AND bucket >= NOW() - (? * INTERVAL '1 month')
            ORDER BY bucket ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, monthlyRowMapper, code, months)
    }

    /**
     * Resumen estadistico (min/max/avg) para un code en un periodo.
     * Agrega sobre la tabla de agregacion apropiada.
     */
    fun getStatisticsSummary(
        code: String,
        count: Int,
        aggregationType: String = "hourly"
    ): Map<String, Any?> {
        val (table, intervalUnit) = when (aggregationType) {
            "hourly" -> "iot.readings_hourly" to "1 hour"
            "daily" -> "iot.readings_daily" to "1 day"
            "weekly" -> "iot.readings_weekly" to "1 week"
            "monthly" -> "iot.readings_monthly" to "1 month"
            else -> "iot.readings_hourly" to "1 hour"
        }

        val sql = """
            SELECT
                AVG(avg_value) AS overall_avg,
                MIN(min_value) AS overall_min,
                MAX(max_value) AS overall_max,
                SUM(count_readings) AS total_readings
            FROM $table
            WHERE code = ?
              AND bucket >= NOW() - (? * INTERVAL '$intervalUnit')
        """.trimIndent()

        return jdbcTemplate.queryForMap(sql, code, count)
    }

    /**
     * Ultimo valor registrado para un code.
     * Lee directamente de sensor_readings (deduplicada).
     */
    fun getLatestValue(code: String): Map<String, Any?>? {
        val sql = """
            SELECT value, time
            FROM iot.sensor_readings
            WHERE code = ?
            ORDER BY time DESC
            LIMIT 1
        """.trimIndent()

        return try {
            jdbcTemplate.queryForMap(sql, code)
        } catch (e: Exception) {
            null
        }
    }
}
