package com.apptolast.invernaderos.features.greenhouse.infrastructure.adapter.output

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseCodeGenerator
import org.springframework.stereotype.Component

@Component
class GreenhouseCodeGeneratorAdapter(
    private val codeGeneratorService: CodeGeneratorService
) : GreenhouseCodeGenerator {

    override fun generate(): String = codeGeneratorService.generateGreenhouseCode()
}
