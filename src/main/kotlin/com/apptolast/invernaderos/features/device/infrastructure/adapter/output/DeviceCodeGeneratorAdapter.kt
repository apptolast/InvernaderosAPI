package com.apptolast.invernaderos.features.device.infrastructure.adapter.output

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceCodeGenerator
import org.springframework.stereotype.Component

@Component
class DeviceCodeGeneratorAdapter(
    private val codeGeneratorService: CodeGeneratorService
) : DeviceCodeGenerator {

    override fun generate(): String = codeGeneratorService.generateDeviceCode()
}
