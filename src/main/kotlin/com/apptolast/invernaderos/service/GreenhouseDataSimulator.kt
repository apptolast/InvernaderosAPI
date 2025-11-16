package com.apptolast.invernaderos.service

import com.apptolast.invernaderos.entities.dtos.RealDataDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * Servicio para generar datos simulados de invernadero con valores realistas
 *
 * Genera datos aleatorios dentro de rangos reales para cada tipo de sensor
 * cuando los sensores físicos no están disponibles (por ejemplo, debido a condiciones climáticas).
 *
 * Rangos realistas:
 * - Temperaturas: 15-35°C (varía según invernadero)
 * - Humedades: 30-90%
 * - Sectores: 0-100% (apertura de ventanas/ventilación)
 * - Extractores: 0.0 (apagado) o 1.0 (encendido)
 * - Reserva: 0-100
 */
@Service
class GreenhouseDataSimulator {

    private val logger = LoggerFactory.getLogger(GreenhouseDataSimulator::class.java)

    /**
     * Genera un objeto RealDataDto con valores aleatorios realistas
     *
     * @param greenhouseId ID del invernadero a simular (default: "001")
     * @return RealDataDto con valores simulados
     */
    fun generateRealisticData(greenhouseId: String = "001"): RealDataDto {
        logger.trace("Generando datos simulados para invernadero: {}", greenhouseId)

        val data = RealDataDto(
            timestamp = Instant.now(),
            greenhouseId = greenhouseId,

            // Temperaturas (cada invernadero tiene su rango característico)
            temperaturaInvernadero01 = generateTemperature(baseTemp = 24.0, variation = 4.0),
            temperaturaInvernadero02 = generateTemperature(baseTemp = 22.0, variation = 4.0),
            temperaturaInvernadero03 = generateTemperature(baseTemp = 25.0, variation = 4.0),

            // Humedades (con variación natural)
            humedadInvernadero01 = generateHumidity(baseHumidity = 65.0, variation = 15.0),
            humedadInvernadero02 = generateHumidity(baseHumidity = 60.0, variation = 15.0),
            humedadInvernadero03 = generateHumidity(baseHumidity = 70.0, variation = 15.0),

            // Sectores (apertura de ventanas 0-100%)
            invernadero01Sector01 = generateSectorValue(),
            invernadero01Sector02 = generateSectorValue(),
            invernadero01Sector03 = generateSectorValue(),
            invernadero01Sector04 = generateSectorValue(),

            invernadero02Sector01 = generateSectorValue(),
            invernadero02Sector02 = generateSectorValue(),
            invernadero02Sector03 = generateSectorValue(),
            invernadero02Sector04 = generateSectorValue(),

            invernadero03Sector01 = generateSectorValue(),
            invernadero03Sector02 = generateSectorValue(),
            invernadero03Sector03 = generateSectorValue(),
            invernadero03Sector04 = generateSectorValue(),

            // Extractores (binario: 0.0 apagado, 1.0 encendido)
            invernadero01Extractor = generateExtractorState(),
            invernadero02Extractor = generateExtractorState(),
            invernadero03Extractor = generateExtractorState(),

            // Reserva
            reserva = generateReservaValue()
        )

        logger.debug("Datos simulados generados - Temp01: {}°C, Hum01: {}%, Greenhouse: {}",
            String.format("%.2f", data.temperaturaInvernadero01),
            String.format("%.2f", data.humedadInvernadero01),
            greenhouseId
        )

        return data
    }

    /**
     * Genera una temperatura realista con distribución normal
     *
     * @param baseTemp Temperatura base (°C)
     * @param variation Variación máxima (°C)
     * @return Temperatura en grados Celsius
     */
    private fun generateTemperature(baseTemp: Double, variation: Double): Double {
        // Usar distribución normal (gaussiana) para valores más realistas
        val randomVariation = nextGaussian() * (variation / 2.0)
        val temperature = baseTemp + randomVariation

        // Clamp entre rangos realistas (15°C - 35°C)
        return temperature.coerceIn(15.0, 35.0)
    }

    /**
     * Genera una humedad realista con distribución normal
     *
     * @param baseHumidity Humedad base (%)
     * @param variation Variación máxima (%)
     * @return Humedad en porcentaje (0-100%)
     */
    private fun generateHumidity(baseHumidity: Double, variation: Double): Double {
        // Usar distribución normal
        val randomVariation = nextGaussian() * (variation / 2.0)
        val humidity = baseHumidity + randomVariation

        // Clamp entre 30% y 90% (rango realista para invernaderos)
        return humidity.coerceIn(30.0, 90.0)
    }

    /**
     * Genera un valor de apertura de sector (ventana/ventilación)
     *
     * Valores más comunes en el rango 30-70% (distribución con tendencia central)
     *
     * @return Valor de apertura en porcentaje (0-100%)
     */
    private fun generateSectorValue(): Double {
        // 70% de probabilidad de estar en rango moderado (30-70%)
        // 30% de probabilidad de estar completamente abierto/cerrado
        return if (Random.nextDouble() < 0.7) {
            // Rango moderado con distribución normal
            val centerValue = 50.0
            val variation = nextGaussian() * 15.0
            (centerValue + variation).coerceIn(30.0, 70.0)
        } else {
            // Valores extremos (completamente abierto o cerrado)
            if (Random.nextBoolean()) 0.0 else 100.0
        }
    }

    /**
     * Genera estado del extractor (binario)
     *
     * @return 0.0 (apagado) o 1.0 (encendido)
     */
    private fun generateExtractorState(): Double {
        // 30% de probabilidad de estar encendido
        return if (Random.nextDouble() < 0.3) 1.0 else 0.0
    }

    /**
     * Genera valor de reserva
     *
     * @return Valor aleatorio entre 0 y 100
     */
    private fun generateReservaValue(): Double {
        return Random.nextDouble() * 100.0
    }

    /**
     * Genera un número con distribución normal (gaussiana)
     *
     * Utiliza ThreadLocalRandom para thread-safety sin necesidad de sincronización
     * Media = 0, Desviación estándar = 1
     */
    private fun nextGaussian(): Double {
        return ThreadLocalRandom.current().nextGaussian()
    }
}
