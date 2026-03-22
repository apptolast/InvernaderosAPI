package com.apptolast.invernaderos.features.sensor.infrastructure.config

import com.apptolast.invernaderos.features.sensor.application.usecase.QuerySensorReadingsUseCaseImpl
import com.apptolast.invernaderos.features.sensor.domain.port.input.QuerySensorReadingsUseCase
import com.apptolast.invernaderos.features.sensor.domain.port.output.SensorReadingQueryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SensorModuleConfig {

    @Bean
    fun querySensorReadingsUseCase(
        queryPort: SensorReadingQueryPort
    ): QuerySensorReadingsUseCase = QuerySensorReadingsUseCaseImpl(queryPort)
}
