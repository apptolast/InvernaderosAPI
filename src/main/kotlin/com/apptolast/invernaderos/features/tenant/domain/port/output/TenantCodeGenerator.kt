package com.apptolast.invernaderos.features.tenant.domain.port.output

interface TenantCodeGenerator {
    fun generate(): String
}
