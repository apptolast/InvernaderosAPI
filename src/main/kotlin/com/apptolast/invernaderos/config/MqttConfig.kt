package com.apptolast.invernaderos.config

import com.apptolast.invernaderos.mqtt.listener.ActuatorStatusListener
import com.apptolast.invernaderos.mqtt.listener.GreenhouseDataListener
import com.apptolast.invernaderos.mqtt.listener.SensorDataListener
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.core.MessageProducer
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
import org.springframework.integration.mqtt.core.MqttPahoClientFactory
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Configuración de MQTT para conectarse con el broker EMQX
 *
 * Basado en la documentación oficial de Spring Integration MQTT:
 * https://docs.spring.io/spring-integration/reference/mqtt.html
 */
@Configuration
class MqttConfig(
    @param:Value("\${spring.mqtt.broker.url}")
    private val brokerUrl: String,

    @param:Value("\${spring.mqtt.username}")
    private val username: String,

    @param:Value("\${spring.mqtt.password}")
    private val password: String,

    @param:Value("\${spring.mqtt.client.id-prefix:greenhouse-api}")
    private val clientIdPrefix: String,

    @param:Value("\${spring.mqtt.client.clean-session:false}")
    private val cleanSession: Boolean,

    @param:Value("\${spring.mqtt.client.connection-timeout:10}")
    private val connectionTimeout: Int,

    @param:Value("\${spring.mqtt.client.keep-alive-interval:60}")
    private val keepAliveInterval: Int,

    @param:Value("\${spring.mqtt.client.automatic-reconnect:true}")
    private val automaticReconnect: Boolean,

    @param:Value("\${spring.mqtt.topics.sensors-pattern:greenhouse/+/sensors/#}")
    private val sensorsTopicPattern: String,

    @param:Value("\${spring.mqtt.topics.actuators-pattern:greenhouse/+/actuators/#}")
    private val actuatorsTopicPattern: String,

    @param:Value("\${spring.mqtt.topics.system-events:system/events/#}")
    private val systemEventsTopicPattern: String,

    @param:Value("\${spring.mqtt.qos.default:0}")
    private val defaultQos: Int,

    private val greenhouseDataListener: GreenhouseDataListener,

    private val sensorDataListener: SensorDataListener,

    private val actuatorStatusListener: ActuatorStatusListener
) {

    private val logger = LoggerFactory.getLogger(MqttConfig::class.java)

    /**
     * Factory para crear clientes MQTT configurados según la documentación oficial
     * https://docs.spring.io/spring-integration/reference/mqtt.html
     */
    @Bean
    fun mqttClientFactory(): MqttPahoClientFactory {
        val factory = DefaultMqttPahoClientFactory()
        val options = MqttConnectOptions()

        // Configurar las opciones de conexión
        options.serverURIs = arrayOf(brokerUrl)
        options.userName = username
        options.password = password.toCharArray()
        options.isCleanSession = cleanSession
        options.connectionTimeout = connectionTimeout
        options.keepAliveInterval = keepAliveInterval
        options.isAutomaticReconnect = automaticReconnect

        factory.connectionOptions = options
        return factory
    }

    // =============================================================================
    // CANALES DE MENSAJERÍA (Message Channels)
    // =============================================================================

    /**
     * Canal para mensajes MQTT entrantes (inbound)
     */
    @Bean
    fun mqttInputChannel(): MessageChannel {
        return DirectChannel()
    }

    /**
     * Canal para mensajes MQTT salientes (outbound)
     */
    @Bean
    fun mqttOutboundChannel(): MessageChannel {
        return DirectChannel()
    }

    // =============================================================================
    // INBOUND ADAPTER - Recibir mensajes del broker MQTT
    // =============================================================================

    /**
     * Adapter para recibir mensajes del broker MQTT
     * Basado en: https://docs.spring.io/spring-integration/reference/mqtt.html#mqtt-inbound
     *
     * Este adapter se suscribe a múltiples topics y envía los mensajes recibidos
     * al canal mqttInputChannel para su procesamiento
     */
    @Bean
    fun mqttInbound(): MessageProducer {
        // Generar un client ID único para el inbound adapter
        val clientId = "$clientIdPrefix-inbound-${UUID.randomUUID()}"

        // Configurar los topics a los que nos suscribimos
        val topics = arrayOf(
            "GREENHOUSE",
            sensorsTopicPattern,
            actuatorsTopicPattern,
            systemEventsTopicPattern
        )

        // Crear el adapter con el client ID, factory y topics
        // El brokerUrl se configura en el mqttClientFactory(), no se pasa aquí
        val adapter = MqttPahoMessageDrivenChannelAdapter(
            clientId,
            mqttClientFactory(),
            *topics
        )

        // Configurar el adapter
        adapter.setCompletionTimeout(5000)
        adapter.setConverter(DefaultPahoMessageConverter())
        adapter.setQos(defaultQos)
        adapter.outputChannel = mqttInputChannel()

        return adapter
    }

    /**
     * Handler que procesa los mensajes MQTT recibidos
     * Se ejecuta cuando llegan mensajes al mqttInputChannel
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    fun mqttMessageHandler(

    ): MessageHandler {
        return MessageHandler { message: Message<*> ->
            try {
                // Extraer información del mensaje PRIMERO (antes de procesarlo)
                val topic = message.headers[MqttHeaders.RECEIVED_TOPIC] as? String ?: ""
                val payload = message.payload as String
                val qos = message.headers[MqttHeaders.RECEIVED_QOS] as? Int

                // Logging de debug ANTES de procesar (para capturar todos los mensajes)
                logger.debug("MQTT message received - Topic: {}, QoS: {}, Payload: {}",
                    topic, qos, payload)

                // Ahora routear el mensaje al listener apropiado
                when {
                    topic == "GREENHOUSE" -> greenhouseDataListener.handleGreenhouseData(message)
                    topic.contains("/sensors/") -> sensorDataListener.handleSensorData(message)
                    topic.contains("/actuators/status") -> actuatorStatusListener.handleActuatorStatus(message)
                    topic.contains("/alerts/") -> logger.info("Alert received on topic: {}", topic)
                    else -> logger.warn("Unhandled topic: {}", topic)
                }

                // TODO: Aquí puedes agregar tu lógica de procesamiento
                // Por ejemplo:
                // - Parsear el payload JSON
                // - Guardar en base de datos
                // - Procesar datos de sensores
                // - Activar actuadores
                // - Enviar notificaciones

            } catch (e: Exception) {
                logger.error("Error processing MQTT message: {}", e.message, e)
                throw e  // Re-throw para que el error channel lo maneje
            }
        }
    }

    // =============================================================================
    // OUTBOUND ADAPTER - Enviar mensajes al broker MQTT
    // =============================================================================

    /**
     * Handler para enviar mensajes al broker MQTT
     * Basado en: https://docs.spring.io/spring-integration/reference/mqtt.html#mqtt-outbound
     *
     * Este handler envía mensajes del canal mqttOutboundChannel al broker MQTT
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    fun mqttOutbound(): MessageHandler {
        // Generar un client ID único para el outbound adapter
        val clientId = "$clientIdPrefix-outbound-${UUID.randomUUID()}"

        // Crear el handler con el client ID y el factory
        val messageHandler = MqttPahoMessageHandler(clientId, mqttClientFactory())

        // Configurar el handler
        messageHandler.setAsync(true) // Envío asíncrono para mejor performance
        messageHandler.setDefaultQos(defaultQos)

        // Nota: El topic se puede especificar por mensaje usando el header MqttHeaders.TOPIC
        // Si no se especifica header, se puede configurar un topic por defecto:
        // messageHandler.setDefaultTopic("default/topic")

        return messageHandler
    }
}