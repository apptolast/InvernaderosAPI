package com.apptolast.invernaderos.features.setting.infrastructure.adapter.output

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingCodeGenerator
import org.springframework.stereotype.Component

@Component
class SettingCodeGeneratorAdapter(
    private val codeGeneratorService: CodeGeneratorService
) : SettingCodeGenerator {

    override fun generate(): String = codeGeneratorService.generateSettingCode()
}
