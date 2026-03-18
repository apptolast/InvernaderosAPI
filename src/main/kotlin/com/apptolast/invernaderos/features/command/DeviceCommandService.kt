package com.apptolast.invernaderos.features.command

import com.apptolast.invernaderos.features.device.DeviceRepository
import com.apptolast.invernaderos.features.setting.SettingRepository
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceCommand
import com.apptolast.invernaderos.features.telemetry.timeseries.DeviceCommandRepository
import com.apptolast.invernaderos.mqtt.publisher.MqttPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Servicio para enviar comandos/consignas desde la app al PLC via MQTT.
 *
 * Flujo: App -> REST -> DeviceCommandService -> TimescaleDB + MQTT -> PLC
 * El PLC no confirma recepcion (fire-and-forget).
 */
@Service
class DeviceCommandService(
    private val deviceCommandRepository: DeviceCommandRepository,
    private val deviceRepository: DeviceRepository,
    private val settingRepository: SettingRepository,
    private val mqttPublisher: MqttPublisher,

    @Value("\${spring.mqtt.topics.response:GREENHOUSE/RESPONSE}")
    private val mqttResponseTopic: String
) {
    private val logger = LoggerFactory.getLogger(DeviceCommandService::class.java)

    /**
     * Envia un comando al PLC.
     *
     * 1. Valida que el code existe en metadata (device o setting)
     * 2. Persiste en iot.device_commands (TimescaleDB)
     * 3. Publica a MQTT topic GREENHOUSE/RESPONSE
     *
     * @param code Codigo del device/setting destino (e.g., SET-00036)
     * @param value Valor del comando como string
     * @return El comando persistido
     * @throws IllegalArgumentException si el code no existe en metadata
     */
    @Transactional("timescaleTransactionManager")
    fun sendCommand(code: String, value: String): DeviceCommand {
        // 1. Validar que el code existe en metadata
        val deviceExists = deviceRepository.findByCode(code) != null
        val settingExists = settingRepository.findByCode(code) != null

        if (!deviceExists && !settingExists) {
            throw IllegalArgumentException("Code '$code' not found in devices or settings")
        }

        // 2. Persistir en TimescaleDB
        val command = DeviceCommand(
            time = Instant.now(),
            code = code,
            value = value
        )
        val savedCommand = deviceCommandRepository.save(command)

        logger.info("Command persisted: code={}, value={}, time={}", code, value, savedCommand.time)

        // 3. Publicar a MQTT (fire-and-forget)
        val mqttPayload = """{"id":"$code","value":$value}"""
        mqttPublisher.publish(mqttResponseTopic, mqttPayload, qos = 1)

        logger.info("Command published to MQTT topic={}: {}", mqttResponseTopic, mqttPayload)

        return savedCommand
    }

    /**
     * Historial de comandos para un code.
     */
    fun getCommandHistory(code: String): List<DeviceCommand> {
        return deviceCommandRepository.findByCodeOrderByTimeDesc(code)
    }

    /**
     * Historial de comandos para un code en un rango temporal.
     */
    fun getCommandHistory(code: String, from: Instant, to: Instant): List<DeviceCommand> {
        return deviceCommandRepository.findByCodeAndTimeBetween(code, from, to)
    }

    /**
     * Ultimo comando enviado para un code.
     */
    fun getLatestCommand(code: String): DeviceCommand? {
        return deviceCommandRepository.findLatestByCode(code)
    }
}
