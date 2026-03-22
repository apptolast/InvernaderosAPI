package com.apptolast.invernaderos.features.tenant.infrastructure.adapter.output

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.tenant.domain.port.output.TenantCodeGenerator
import org.springframework.stereotype.Component

@Component
class TenantCodeGeneratorAdapter(
    private val codeGeneratorService: CodeGeneratorService
) : TenantCodeGenerator {

    override fun generate(): String = codeGeneratorService.generateTenantCode()
}
