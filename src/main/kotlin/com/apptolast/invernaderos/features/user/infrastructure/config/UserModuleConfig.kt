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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserModuleConfig {

    @Bean
    fun createUserUseCase(
        repository: UserRepositoryPort,
        codeGenerator: UserCodeGenerator,
        passwordHasher: PasswordHasher
    ): CreateUserUseCase = CreateUserUseCaseImpl(repository, codeGenerator, passwordHasher)

    @Bean
    fun findUserUseCase(
        repository: UserRepositoryPort
    ): FindUserUseCase = FindUserUseCaseImpl(repository)

    @Bean
    fun updateUserUseCase(
        repository: UserRepositoryPort,
        passwordHasher: PasswordHasher
    ): UpdateUserUseCase = UpdateUserUseCaseImpl(repository, passwordHasher)

    @Bean
    fun deleteUserUseCase(
        repository: UserRepositoryPort
    ): DeleteUserUseCase = DeleteUserUseCaseImpl(repository)
}
