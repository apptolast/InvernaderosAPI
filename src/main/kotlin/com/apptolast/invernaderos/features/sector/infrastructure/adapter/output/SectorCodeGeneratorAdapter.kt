package com.apptolast.invernaderos.features.sector.infrastructure.adapter.output

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorCodeGenerator
import org.springframework.stereotype.Component

@Component
class SectorCodeGeneratorAdapter(
    private val codeGeneratorService: CodeGeneratorService
) : SectorCodeGenerator {

    override fun generate(): String = codeGeneratorService.generateSectorCode()
}
