package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.actuator.ActuatorStateRepository
import com.apptolast.invernaderos.features.actuator.ActuatorTypeRepository
import com.apptolast.invernaderos.features.alert.AlertSeverityRepository
import com.apptolast.invernaderos.features.alert.AlertTypeRepository
import com.apptolast.invernaderos.features.catalog.catalog.ActuatorState
import com.apptolast.invernaderos.features.catalog.catalog.ActuatorType
import com.apptolast.invernaderos.features.catalog.catalog.AlertSeverity
import com.apptolast.invernaderos.features.catalog.catalog.AlertType
import com.apptolast.invernaderos.features.catalog.catalog.SensorType
import com.apptolast.invernaderos.features.catalog.catalog.Unit as CatalogUnit
import com.apptolast.invernaderos.features.sensor.SensorTypeRepository
import com.apptolast.invernaderos.features.sensor.UnitRepository
import java.util.Optional
import org.springframework.stereotype.Service

@Service
class CatalogService(
        private val unitRepository: UnitRepository,
        private val sensorTypeRepository: SensorTypeRepository,
        private val actuatorTypeRepository: ActuatorTypeRepository,
        private val actuatorStateRepository: ActuatorStateRepository,
        private val alertSeverityRepository: AlertSeverityRepository,
        private val alertTypeRepository: AlertTypeRepository
) {

    fun getAllUnits(activeOnly: Boolean): List<CatalogUnit> {
        return if (activeOnly) {
            unitRepository.findByIsActiveTrue()
        } else {
            unitRepository.findAll()
        }
    }

    fun getUnitById(id: Short): Optional<CatalogUnit> {
        return unitRepository.findById(id)
    }

    fun getAllSensorTypes(activeOnly: Boolean): List<SensorType> {
        return if (activeOnly) {
            sensorTypeRepository.findByIsActiveTrue()
        } else {
            sensorTypeRepository.findAll()
        }
    }

    fun getSensorTypeById(id: Short): Optional<SensorType> {
        return sensorTypeRepository.findById(id)
    }

    fun getAllActuatorTypes(activeOnly: Boolean): List<ActuatorType> {
        return if (activeOnly) {
            actuatorTypeRepository.findByIsActiveTrue()
        } else {
            actuatorTypeRepository.findAll()
        }
    }

    fun getActuatorTypeById(id: Short): Optional<ActuatorType> {
        return actuatorTypeRepository.findById(id)
    }

    fun getAllActuatorStates(operationalOnly: Boolean): List<ActuatorState> {
        return if (operationalOnly) {
            actuatorStateRepository.findByIsOperationalTrue()
        } else {
            actuatorStateRepository.findAllOrderedByDisplay()
        }
    }

    fun getActuatorStateById(id: Short): Optional<ActuatorState> {
        return actuatorStateRepository.findById(id)
    }

    fun getAllAlertSeverities(): List<AlertSeverity> {
        return alertSeverityRepository.findAllOrderedByLevel()
    }

    fun getAlertSeverityById(id: Short): Optional<AlertSeverity> {
        return alertSeverityRepository.findById(id)
    }

    fun getAllAlertTypes(activeOnly: Boolean, category: String?): List<AlertType> {
        return when {
            category != null && activeOnly ->
                    alertTypeRepository.findByCategoryAndIsActiveTrue(category)
            category != null -> alertTypeRepository.findByCategory(category)
            activeOnly -> alertTypeRepository.findByIsActiveTrue()
            else -> alertTypeRepository.findAll()
        }
    }

    fun getAlertTypeById(id: Short): Optional<AlertType> {
        return alertTypeRepository.findById(id)
    }
}
