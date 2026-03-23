package com.apptolast.invernaderos.features.greenhouse.domain.port.input

import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.Location
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.math.BigDecimal

interface CreateGreenhouseUseCase {
    fun execute(command: CreateGreenhouseCommand): Either<GreenhouseError, Greenhouse>
}

data class CreateGreenhouseCommand(
    val tenantId: TenantId,
    val name: String,
    val location: Location? = null,
    val areaM2: BigDecimal? = null,
    val timezone: String? = "Europe/Madrid",
    val isActive: Boolean = true
)
