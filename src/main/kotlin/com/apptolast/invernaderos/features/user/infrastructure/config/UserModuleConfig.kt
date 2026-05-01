package com.apptolast.invernaderos.features.user.infrastructure.config

import com.apptolast.invernaderos.features.user.application.usecase.CreateUserUseCaseImpl
import com.apptolast.invernaderos.features.user.application.usecase.DeleteUserUseCaseImpl
import com.apptolast.invernaderos.features.user.application.usecase.FindUserUseCaseImpl
import com.apptolast.invernaderos.features.user.application.usecase.UpdateUserUseCaseImpl
import com.apptolast.invernaderos.features.user.domain.port.input.CreateUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.input.DeleteUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.input.FindUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.input.UpdateUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.output.PasswordHasher
import com.apptolast.invernaderos.features.user.domain.port.output.UserCodeGenerator
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserModuleConfig {

    @Bean
    fun createUserUseCase(
        repository: UserRepositoryPort,
        codeGenerator: UserCodeGenerator,
        passwordHasher: PasswordHasher,
        applicationEventPublisher: ApplicationEventPublisher
    ): CreateUserUseCase = CreateUserUseCaseImpl(repository, codeGenerator, passwordHasher, applicationEventPublisher)

    @Bean
    fun findUserUseCase(
        repository: UserRepositoryPort
    ): FindUserUseCase = FindUserUseCaseImpl(repository)

    @Bean
    fun updateUserUseCase(
        repository: UserRepositoryPort,
        passwordHasher: PasswordHasher,
        applicationEventPublisher: ApplicationEventPublisher
    ): UpdateUserUseCase = UpdateUserUseCaseImpl(repository, passwordHasher, applicationEventPublisher)

    @Bean
    fun deleteUserUseCase(
        repository: UserRepositoryPort,
        applicationEventPublisher: ApplicationEventPublisher
    ): DeleteUserUseCase = DeleteUserUseCaseImpl(repository, applicationEventPublisher)
}
