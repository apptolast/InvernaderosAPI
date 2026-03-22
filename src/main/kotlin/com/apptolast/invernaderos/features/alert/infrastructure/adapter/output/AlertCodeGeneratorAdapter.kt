package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertCodeGenerator
import org.springframework.stereotype.Component

@Component
class AlertCodeGeneratorAdapter(
    private val codeGeneratorService: CodeGeneratorService
) : AlertCodeGenerator {

    override fun generate(): String = codeGeneratorService.generateAlertCode()
}
