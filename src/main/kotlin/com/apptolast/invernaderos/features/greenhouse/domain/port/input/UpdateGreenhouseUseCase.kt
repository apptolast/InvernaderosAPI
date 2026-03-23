package com.apptolast.invernaderos.features.greenhouse.domain.port.input

import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.Location
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.math.BigDecimal

interface UpdateGreenhouseUseCase {
    fun execute(command: UpdateGreenhouseCommand): Either<GreenhouseError, Greenhouse>
}

data class UpdateGreenhouseCommand(
    val id: GreenhouseId,
    val tenantId: TenantId,
    val name: String? = null,
    val location: Location? = null,
    val areaM2: BigDecimal? = null,
    val timezone: String? = null,
    val isActive: Boolean? = null
)
