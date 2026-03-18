package com.apptolast.invernaderos.features.command

import com.apptolast.invernaderos.features.telemetry.timeseries.DeviceCommandRepository
import com.apptolast.invernaderos.mqtt.publisher.MqttPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Broadcaster continuo de comandos al PLC via MQTT.
 *
 * Cada 5 segundos lee el ultimo valor de cada code en device_commands
 * y lo publica a GREENHOUSE/RESPONSE. Esto permite que el PLC reciba
 * continuamente los comandos/consignas actuales, igual que la API
 * recibe continuamente los valores de sensores del PLC via GREENHOUSE/STATUS.
 *
 * Flujo simetrico:
 * - PLC → GREENHOUSE/STATUS → API  (valores de sensores, cada segundo)
 * - API → GREENHOUSE/RESPONSE → PLC (comandos/consignas, cada 5 segundos)
 */
@Service
class DeviceCommandBroadcaster(
    private val deviceCommandRepository: DeviceCommandRepository,
    private val mqttPublisher: MqttPublisher,

    @Value("\${spring.mqtt.topics.response:GREENHOUSE/RESPONSE}")
    private val mqttResponseTopic: String
) {
    private val logger = LoggerFactory.getLogger(DeviceCommandBroadcaster::class.java)

    /**
     * Publica todos los comandos actuales al MQTT cada 5 segundos.
     * Lee el ultimo valor de cada code en device_commands y lo publica
     * como mensaje individual: {"id":"SET-00036","value":15}
     */
    @Scheduled(fixedRate = 5000)
    @Transactional("timescaleTransactionManager", readOnly = true)
    fun broadcastCommands() {
        val latestCommands = deviceCommandRepository.findLatestForAllCodes()

        if (latestCommands.isEmpty()) {
            return
        }

        var publishedCount = 0
        for (command in latestCommands) {
            val payload = """{"id":"${command.code}","value":${command.value}}"""
            try {
                mqttPublisher.publish(mqttResponseTopic, payload, qos = 0)
                publishedCount++
            } catch (e: Exception) {
                logger.error("Failed to broadcast command code={}: {}", command.code, e.message)
            }
        }

        logger.debug("Broadcast {} commands to {}", publishedCount, mqttResponseTopic)
    }
}
