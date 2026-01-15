package com.apptolast.invernaderos.features.statistics.dao

import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsDailyDto
import com.apptolast.invernaderos.features.telemetry.timescaledb.dto.SensorStatisticsHourlyDto
import java.sql.ResultSet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

/**
 * Repository para consultas de estadísticas agregadas en TimescaleDB.
 *
 * Usa JDBC nativo para queries a tablas de agregación:
 * - iot.sensor_readings_hourly (agregaciones horarias)
 * - iot.sensor_readings_daily (agregaciones diarias)
 */
class StatsDao(@Qualifier("timescaleJdbcTemplate") private val jdbcTemplate: JdbcTemplate) {

    // ==================== ROW MAPPERS ====================

    private val hourlyRowMapper = RowMapper { rs: ResultSet, _: Int ->
        SensorStatisticsHourlyDto(
                bucket = rs.getTimestamp("bucket").toInstant(),
                greenhouseId = rs.getLong("greenhouse_id"),
                tenantId = rs.getLong("tenant_id"),
                sensorType = rs.getString("sensor_type"),
                unit = rs.getString("unit"),
                avgValue = rs.getDouble("avg_value").takeUnless { rs.wasNull() },
                minValue = rs.getDouble("min_value").takeUnless { rs.wasNull() },
                maxValue = rs.getDouble("max_value").takeUnless { rs.wasNull() },
                stddevValue = rs.getDouble("stddev_value").takeUnless { rs.wasNull() },
                countReadings = rs.getLong("count_readings"),
                nullCount = rs.getLong("null_count").takeUnless { rs.wasNull() },
                firstReadingAt = rs.getTimestamp("first_reading_at")?.toInstant(),
                lastReadingAt = rs.getTimestamp("last_reading_at")?.toInstant()
        )
    }

    private val dailyRowMapper = RowMapper { rs: ResultSet, _: Int ->
        SensorStatisticsDailyDto(
                bucket = rs.getTimestamp("bucket").toInstant(),
                greenhouseId = rs.getLong("greenhouse_id"),
                tenantId = rs.getLong("tenant_id"),
                sensorType = rs.getString("sensor_type"),
                unit = rs.getString("unit"),
                avgValue = rs.getDouble("avg_value").takeUnless { rs.wasNull() },
                minValue = rs.getDouble("min_value").takeUnless { rs.wasNull() },
                maxValue = rs.getDouble("max_value").takeUnless { rs.wasNull() },
                stddevValue = rs.getDouble("stddev_value").takeUnless { rs.wasNull() },
                countReadings = rs.getLong("count_readings"),
                medianValue = rs.getDouble("median_value").takeUnless { rs.wasNull() },
                p95Value = rs.getDouble("p95_value").takeUnless { rs.wasNull() },
                p5Value = rs.getDouble("p5_value").takeUnless { rs.wasNull() },
                nullCount = rs.getLong("null_count").takeUnless { rs.wasNull() },
                hoursWithData = rs.getShort("hours_with_data").takeUnless { rs.wasNull() },
                firstReadingAt = rs.getTimestamp("first_reading_at")?.toInstant(),
                lastReadingAt = rs.getTimestamp("last_reading_at")?.toInstant()
        )
    }

    // ==================== HOURLY QUERIES ====================

    /**
     * GET /api/statistics/hourly?greenhouseId=xxx&sensorType=TEMPERATURE&hours=24
     *
     * Obtiene estadísticas horarias de un sensor. Usado para gráfica "Últimas 24h" en pantalla
     * Historial de Datos.
     */
    fun getHourlyStatistics(
            greenhouseId: Long,
            sensorType: String,
            hours: Int = 24
    ): List<SensorStatisticsHourlyDto> {
        val sql =
                """
            SELECT
                time AS bucket,
                greenhouse_id,
                tenant_id,
                sensor_type,
                unit,
                avg_value,
                min_value,
                max_value,
                stddev_value,
                count_readings,
                null_count,
                first_reading_at,
                last_reading_at
            FROM iot.sensor_readings_hourly
            WHERE greenhouse_id = ?
              AND sensor_type = ?
              AND time >= NOW() - (? * INTERVAL '1 hour')
            ORDER BY time ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, hourlyRowMapper, greenhouseId, sensorType, hours)
    }

    /** Obtiene estadísticas horarias con filtro por tenant. */
    fun getHourlyStatisticsByTenant(
            tenantId: Long,
            greenhouseId: Long?,
            sensorType: String,
            hours: Int = 24
    ): List<SensorStatisticsHourlyDto> {
        val sql =
                if (greenhouseId != null) {
                    """
            SELECT time AS bucket, greenhouse_id, tenant_id, sensor_type, unit,
                   avg_value, min_value, max_value, stddev_value, count_readings,
                   null_count, first_reading_at, last_reading_at
            FROM iot.sensor_readings_hourly
            WHERE tenant_id = ?
              AND greenhouse_id = ?
              AND sensor_type = ?
              AND time >= NOW() - (? * INTERVAL '1 hour')
            ORDER BY time ASC
            """.trimIndent()
                } else {
                    """
            SELECT time AS bucket, greenhouse_id, tenant_id, sensor_type, unit,
                   avg_value, min_value, max_value, stddev_value, count_readings,
                   null_count, first_reading_at, last_reading_at
            FROM iot.sensor_readings_hourly
            WHERE tenant_id = ?
              AND sensor_type = ?
              AND time >= NOW() - (? * INTERVAL '1 hour')
            ORDER BY time ASC
            """.trimIndent()
                }

        return if (greenhouseId != null) {
            jdbcTemplate.query(sql, hourlyRowMapper, tenantId, greenhouseId, sensorType, hours)
        } else {
            jdbcTemplate.query(sql, hourlyRowMapper, tenantId, sensorType, hours)
        }
    }

    // ==================== DAILY QUERIES ====================

    /**
     * GET /api/statistics/daily?greenhouseId=xxx&sensorType=TEMPERATURE&days=7
     *
     * Obtiene estadísticas diarias de un sensor. Usado para gráfica "Últimos 7 días" en pantalla
     * Historial de Datos.
     */
    fun getDailyStatistics(
            greenhouseId: Long,
            sensorType: String,
            days: Int = 7
    ): List<SensorStatisticsDailyDto> {
        val sql =
                """
            SELECT
                time AS bucket,
                greenhouse_id,
                tenant_id,
                sensor_type,
                unit,
                avg_value,
                min_value,
                max_value,
                stddev_value,
                count_readings,
                median_value,
                p95_value,
                p5_value,
                null_count,
                hours_with_data,
                first_reading_at,
                last_reading_at
            FROM iot.sensor_readings_daily
            WHERE greenhouse_id = ?
              AND sensor_type = ?
              AND time >= NOW() - (? * INTERVAL '1 day')
            ORDER BY time ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, dailyRowMapper, greenhouseId, sensorType, days)
    }

    /** Obtiene estadísticas diarias con filtro por tenant. */
    fun getDailyStatisticsByTenant(
            tenantId: Long,
            greenhouseId: Long?,
            sensorType: String,
            days: Int = 7
    ): List<SensorStatisticsDailyDto> {
        val sql =
                if (greenhouseId != null) {
                    """
            SELECT time AS bucket, greenhouse_id, tenant_id, sensor_type, unit,
                   avg_value, min_value, max_value, stddev_value, count_readings,
                   median_value, p95_value, p5_value, null_count, hours_with_data,
                   first_reading_at, last_reading_at
            FROM iot.sensor_readings_daily
            WHERE tenant_id = ?
              AND greenhouse_id = ?
              AND sensor_type = ?
              AND time >= NOW() - (? * INTERVAL '1 day')
            ORDER BY time ASC
            """.trimIndent()
                } else {
                    """
            SELECT time AS bucket, greenhouse_id, tenant_id, sensor_type, unit,
                   avg_value, min_value, max_value, stddev_value, count_readings,
                   median_value, p95_value, p5_value, null_count, hours_with_data,
                   first_reading_at, last_reading_at
            FROM iot.sensor_readings_daily
            WHERE tenant_id = ?
              AND sensor_type = ?
              AND time >= NOW() - (? * INTERVAL '1 day')
            ORDER BY time ASC
            """.trimIndent()
                }

        return if (greenhouseId != null) {
            jdbcTemplate.query(sql, dailyRowMapper, tenantId, greenhouseId, sensorType, days)
        } else {
            jdbcTemplate.query(sql, dailyRowMapper, tenantId, sensorType, days)
        }
    }

    // ==================== STATISTICS SUMMARY ====================

    /**
     * GET /api/statistics/summary?greenhouseId=xxx&sensorType=TEMPERATURE&period=24h
     *
     * Obtiene resumen estadístico (min/max/avg) para un período. Usado para las cards de
     * "Promedio", "Máx", "Mín" en pantalla Historial de Datos.
     */
    fun getStatisticsSummary(
            greenhouseId: Long,
            sensorType: String,
            hoursOrDays: Int,
            aggregationType: String = "hourly" // "hourly" or "daily"
    ): Map<String, Any?> {
        val table =
                if (aggregationType == "hourly") {
                    "iot.sensor_readings_hourly"
                } else {
                    "iot.sensor_readings_daily"
                }

        val intervalClause =
                if (aggregationType == "hourly") {
                    "INTERVAL '1 hour'"
                } else {
                    "INTERVAL '1 day'"
                }

        val sql =
                """
            SELECT
                AVG(avg_value) AS overall_avg,
                MIN(min_value) AS overall_min,
                MAX(max_value) AS overall_max,
                MAX(unit) AS unit,
                SUM(count_readings) AS total_readings
            FROM $table
            WHERE greenhouse_id = ?
              AND sensor_type = ?
              AND time >= NOW() - (? * $intervalClause)
        """.trimIndent()

        return jdbcTemplate.queryForMap(sql, greenhouseId, sensorType, hoursOrDays)
    }

    // ==================== LATEST VALUE QUERY ====================

    /**
     * GET /api/statistics/latest?greenhouseId=xxx&sensorType=TEMPERATURE
     *
     * Obtiene el último valor de un sensor (para el "22.5°C" en la UI).
     */
    fun getLatestValue(greenhouseId: Long, sensorType: String): Map<String, Any?>? {
        val sql =
                """
            SELECT
                value,
                time,
                unit
            FROM iot.sensor_readings
            WHERE greenhouse_id = ?
              AND sensor_type = ?
            ORDER BY time DESC
            LIMIT 1
        """.trimIndent()

        return try {
            jdbcTemplate.queryForMap(sql, greenhouseId, sensorType)
        } catch (e: Exception) {
            null
        }
    }
}
