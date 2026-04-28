package com.apptolast.invernaderos.features.alert.domain.error

import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

sealed interface AlertError {
    val message: String

    data class NotFound(val id: Long, val tenantId: TenantId) : AlertError {
        override val message: String
            get() = "Alert $id not found for tenant ${tenantId.value}"
    }

    data class SectorNotOwnedByTenant(val sectorId: SectorId, val tenantId: TenantId) : AlertError {
        override val message: String
            get() = "Sector ${sectorId.value} does not belong to tenant ${tenantId.value}"
    }

    data class AlreadyResolved(val id: Long) : AlertError {
        override val message: String
            get() = "Alert $id is already resolved"
    }

    data class NotResolved(val id: Long) : AlertError {
        override val message: String
            get() = "Alert $id is already open (not resolved)"
    }

    data class UnknownCode(val code: String) : AlertError {
        override val message: String
            get() = "Alert with code '$code' not found - MQTT signal ignored (strict mode)"
    }

    data class NoTransitionRequired(val id: Long, val currentlyResolved: Boolean) : AlertError {
        override val message: String
            get() = "Alert $id already in target state (isResolved=$currentlyResolved) - signal is a NO_OP"
    }

    data class InvalidSignalValue(val code: String, val rawValue: String) : AlertError {
        override val message: String
            get() = "Cannot map MQTT value '$rawValue' for alert '$code' to a known decision"
    }
}
