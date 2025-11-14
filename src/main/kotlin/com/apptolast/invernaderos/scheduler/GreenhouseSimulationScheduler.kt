package com.apptolast.invernaderos.scheduler

import com.apptolast.invernaderos.entities.dtos.toJson
import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import com.apptolast.invernaderos.service.GreenhouseDataSimulator
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Scheduler para simular datos de invernadero de forma automática
 *
 * Este componente genera datos simulados de sensores a intervalos regulares
 * cuando los sensores físicos no están disponibles (por ejemplo, por condiciones climáticas).
 *
 * Activación:
 * - Se activa configurando `greenhouse.simulation.enabled=true` en application.yaml
 * - Por defecto está desactivado para evitar generar datos simulados en producción accidentalmente
 *
 * Configuración:
 * ```yaml
 * greenhouse:
 *   simulation:
 *     enabled: true          # Activar/desactivar simulación
 *     interval-ms: 5000      # Intervalo en milisegundos
 *     greenhouse-id: "001"   # ID del invernadero a simular
 * ```
 *
 * Funcionamiento:
 * 1. Cada 5 segundos (configurable), genera datos aleatorios realistas
 * 2. Los datos pasan por el mismo flujo que los datos reales:
 *    - Se cachean en Redis
 *    - Se guardan en TimescaleDB
 *    - Se publican via WebSocket a los clientes
 *
 * Esto garantiza que la aplicación móvil reciba datos continuamente
 * incluso cuando los sensores físicos están fuera de servicio.
 */
@Component
@ConditionalOnProperty(
    prefix = "greenhouse.simulation",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class GreenhouseSimulationScheduler(
    private val dataSimulator: GreenhouseDataSimulator,
    private val messageProcessor: MqttMessageProcessor
) {

    private val logger = LoggerFactory.getLogger(GreenhouseSimulationScheduler::class.java)

    companion object {
        private const val DEFAULT_GREENHOUSE_ID = "001"
    }

    // Variable para trackear si es la primera ejecución
    @Volatile
    private var isFirstRun = true

    /**
     * Tarea programada que genera y procesa datos simulados cada 5 segundos
     *
     * Usa @Scheduled con fixedRate para garantizar ejecuciones constantes
     * independientemente del tiempo de procesamiento.
     *
     * fixedRate: El intervalo se mide desde el INICIO de cada ejecución
     * (a diferencia de fixedDelay que mide desde el FIN)
     */
    @Scheduled(fixedRate = 5000, timeUnit = TimeUnit.MILLISECONDS)
    fun generateAndPublishSimulatedData() {
        try {
            // Log informativo solo en la primera ejecución para no saturar logs
            if (isFirstRun) {
                logger.info("========================================")
                logger.info("INICIANDO SIMULACIÓN DE DATOS DE INVERNADERO")
                logger.info("Generando datos cada 5 segundos")
                logger.info("Greenhouse ID: {}", DEFAULT_GREENHOUSE_ID)
                logger.info("========================================")
                isFirstRun = false
            }

            logger.debug("Generando datos simulados - ciclo de ejecución")

            // 1. Generar datos realistas usando el simulador
            val simulatedData = dataSimulator.generateRealisticData(DEFAULT_GREENHOUSE_ID)

            // 2. Convertir a JSON (el formato que espera MqttMessageProcessor)
            //    Los @JsonProperty en RealDataDto aseguran el formato correcto:
            //    - "TEMPERATURA INVERNADERO 01" (con espacios)
            //    - "INVERNADERO_01_SECTOR_01" (con underscores)
            val jsonPayload = simulatedData.toJson()

            logger.trace("Payload JSON generado: {}", jsonPayload)

            // 3. Procesar usando el mismo flujo que datos reales
            //    Esto garantiza:
            //    - Cache en Redis (últimos 1000 mensajes)
            //    - Persistencia en TimescaleDB
            //    - Publicación de GreenhouseMessageEvent → WebSocket
            messageProcessor.processGreenhouseData(jsonPayload, DEFAULT_GREENHOUSE_ID)

            logger.debug(
                "Datos simulados procesados exitosamente - Temp01: {}°C, Hum01: {}%",
                String.format("%.2f", simulatedData.temperaturaInvernadero01),
                String.format("%.2f", simulatedData.humedadInvernadero01)
            )

        } catch (e: Exception) {
            logger.error("Error generando/procesando datos simulados: ${e.message}", e)
            // No relanzar la excepción para que el scheduler continúe ejecutándose
            // El siguiente ciclo intentará generar datos nuevamente
        }
    }

    /**
     * Hook de inicialización para logging
     *
     * Se ejecuta cuando Spring crea el bean, solo si la simulación está habilitada
     */
    init {
        logger.warn("╔════════════════════════════════════════════════════════════╗")
        logger.warn("║  SIMULACIÓN DE DATOS DE INVERNADERO ACTIVADA              ║")
        logger.warn("║                                                            ║")
        logger.warn("║  Los datos mostrados son SIMULADOS, no provienen de       ║")
        logger.warn("║  sensores reales. Esto es debido a que los sensores       ║")
        logger.warn("║  físicos están fuera de servicio.                         ║")
        logger.warn("║                                                            ║")
        logger.warn("║  Para desactivar: greenhouse.simulation.enabled=false     ║")
        logger.warn("╚════════════════════════════════════════════════════════════╝")
    }
}
