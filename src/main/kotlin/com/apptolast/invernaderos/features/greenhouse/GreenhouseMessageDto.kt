package com.apptolast.invernaderos.features.greenhouse

import java.time.Instant

/**
 * DTO para mensajes recibidos del topic MQTT GREENHOUSE
 *
 * Formato del mensaje JSON:
 * {
 *   "SENSOR_01": 1.23,
 *   "SENSOR_02": 2.23,
 *   "SETPOINT_01": 0.1,
 *   "SETPOINT_02": 0.2,
 *   "SETPOINT_03": 0.3
 * }
 */
data class GreenhouseMessageDto(
    val timestamp: Instant,
    val sensor01: Double?,
    val sensor02: Double?,
    val setpoint01: Double?,
    val setpoint02: Double?,
    val setpoint03: Double?,
    val greenhouseId: String? = null,
    val rawPayload: String? = null
) {
    /**
     * Convierte el DTO a un Map para facilitar el acceso dinámico a los valores
     */
    fun toValueMap(): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        sensor01?.let { map["SENSOR_01"] = it }
        sensor02?.let { map["SENSOR_02"] = it }
        setpoint01?.let { map["SETPOINT_01"] = it }
        setpoint02?.let { map["SETPOINT_02"] = it }
        setpoint03?.let { map["SETPOINT_03"] = it }
        return map
    }

    /**
     * Devuelve todos los sensores (SENSOR_XX)
     */
    fun getSensors(): Map<String, Double> {
        val sensors = mutableMapOf<String, Double>()
        sensor01?.let { sensors["SENSOR_01"] = it }
        sensor02?.let { sensors["SENSOR_02"] = it }
        return sensors
    }

    /**
     * Devuelve todos los setpoints (SETPOINT_XX)
     */
    fun getSetpoints(): Map<String, Double> {
        val setpoints = mutableMapOf<String, Double>()
        setpoint01?.let { setpoints["SETPOINT_01"] = it }
        setpoint02?.let { setpoints["SETPOINT_02"] = it }
        setpoint03?.let { setpoints["SETPOINT_03"] = it }
        return setpoints
    }

    /**
     * Random Data
     */
    fun randomDatafromGreenHouseTopic(): GreenhouseMessageDto {
        return this.copy(
            setpoint01 = this.setpoint01?.let { it + (0.9 + Math.random() + 0.2) },
            setpoint02 = this.setpoint02?.let { it + (0.3 + Math.random() + 0.8) },
            setpoint03 = this.setpoint03?.let { it + (0.1 + Math.random() + 0.7) },
        )
    }
}
