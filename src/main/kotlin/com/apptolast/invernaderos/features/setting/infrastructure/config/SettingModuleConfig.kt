package com.apptolast.invernaderos.features.setting.infrastructure.config

import com.apptolast.invernaderos.features.setting.application.usecase.CreateSettingUseCaseImpl
import com.apptolast.invernaderos.features.setting.application.usecase.DeleteSettingUseCaseImpl
import com.apptolast.invernaderos.features.setting.application.usecase.FindSettingUseCaseImpl
import com.apptolast.invernaderos.features.setting.application.usecase.UpdateSettingUseCaseImpl
import com.apptolast.invernaderos.features.setting.domain.port.input.CreateSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.input.DeleteSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.input.FindSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.input.UpdateSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingCodeGenerator
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingSectorValidationPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SettingModuleConfig {

    @Bean
    fun createSettingUseCase(
        repository: SettingRepositoryPort,
        codeGenerator: SettingCodeGenerator,
        sectorValidation: SettingSectorValidationPort
    ): CreateSettingUseCase = CreateSettingUseCaseImpl(repository, codeGenerator, sectorValidation)

    @Bean
    fun findSettingUseCase(
        repository: SettingRepositoryPort
    ): FindSettingUseCase = FindSettingUseCaseImpl(repository)

    @Bean
    fun updateSettingUseCase(
        repository: SettingRepositoryPort,
        sectorValidation: SettingSectorValidationPort
    ): UpdateSettingUseCase = UpdateSettingUseCaseImpl(repository, sectorValidation)

    @Bean
    fun deleteSettingUseCase(
        repository: SettingRepositoryPort
    ): DeleteSettingUseCase = DeleteSettingUseCaseImpl(repository)
}
