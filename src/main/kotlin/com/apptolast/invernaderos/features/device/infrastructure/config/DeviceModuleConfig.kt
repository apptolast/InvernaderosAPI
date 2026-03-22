package com.apptolast.invernaderos.features.device.infrastructure.config

import com.apptolast.invernaderos.features.device.application.usecase.CreateDeviceUseCaseImpl
import com.apptolast.invernaderos.features.device.application.usecase.DeleteDeviceUseCaseImpl
import com.apptolast.invernaderos.features.device.application.usecase.FindCommandHistoryUseCaseImpl
import com.apptolast.invernaderos.features.device.application.usecase.FindDeviceUseCaseImpl
import com.apptolast.invernaderos.features.device.application.usecase.UpdateDeviceUseCaseImpl
import com.apptolast.invernaderos.features.device.domain.port.input.CreateDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.input.DeleteDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.input.FindCommandHistoryUseCase
import com.apptolast.invernaderos.features.device.domain.port.input.FindDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.input.UpdateDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.output.CommandHistoryRepositoryPort
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceCodeGenerator
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
import com.apptolast.invernaderos.features.device.domain.port.output.SectorExistencePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DeviceModuleConfig {

    @Bean
    fun createDeviceUseCase(
        repository: DeviceRepositoryPort,
        codeGenerator: DeviceCodeGenerator,
        sectorExistence: SectorExistencePort
    ): CreateDeviceUseCase = CreateDeviceUseCaseImpl(repository, codeGenerator, sectorExistence)

    @Bean
    fun findDeviceUseCase(
        repository: DeviceRepositoryPort
    ): FindDeviceUseCase = FindDeviceUseCaseImpl(repository)

    @Bean
    fun updateDeviceUseCase(
        repository: DeviceRepositoryPort,
        sectorExistence: SectorExistencePort
    ): UpdateDeviceUseCase = UpdateDeviceUseCaseImpl(repository, sectorExistence)

    @Bean
    fun deleteDeviceUseCase(
        repository: DeviceRepositoryPort
    ): DeleteDeviceUseCase = DeleteDeviceUseCaseImpl(repository)

    @Bean
    fun findCommandHistoryUseCase(
        repository: CommandHistoryRepositoryPort
    ): FindCommandHistoryUseCase = FindCommandHistoryUseCaseImpl(repository)
}
