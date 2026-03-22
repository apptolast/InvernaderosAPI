package com.apptolast.invernaderos.features.user.infrastructure.adapter.output

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.user.domain.port.output.UserCodeGenerator
import org.springframework.stereotype.Component

@Component
class UserCodeGeneratorAdapter(
    private val codeGeneratorService: CodeGeneratorService
) : UserCodeGenerator {

    override fun generate(): String = codeGeneratorService.generateUserCode()
}
