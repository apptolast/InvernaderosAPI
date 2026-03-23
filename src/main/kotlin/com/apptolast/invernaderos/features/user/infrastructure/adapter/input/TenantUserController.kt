package com.apptolast.invernaderos.features.user.infrastructure.adapter.input

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.port.input.CreateUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.input.DeleteUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.input.FindUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.input.UpdateUserUseCase
import com.apptolast.invernaderos.features.user.dto.mapper.toCommand
import com.apptolast.invernaderos.features.user.dto.mapper.toResponse
import com.apptolast.invernaderos.features.user.dto.request.UserCreateRequest
import com.apptolast.invernaderos.features.user.dto.request.UserUpdateRequest
import com.apptolast.invernaderos.features.user.dto.response.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/users")
@Tag(name = "User Management", description = "Endpoints para la gestión de usuarios de un cliente")
class TenantUserController(
    private val createUseCase: CreateUserUseCase,
    private val findUseCase: FindUserUseCase,
    private val updateUseCase: UpdateUserUseCase,
    private val deleteUseCase: DeleteUserUseCase
) {

    @GetMapping
    @Operation(summary = "Obtener todos los usuarios de un cliente")
    fun getAllUsers(@PathVariable tenantId: Long): ResponseEntity<List<UserResponse>> {
        val users = findUseCase.findAllByTenantId(TenantId(tenantId))
        return ResponseEntity.ok(users.map { it.toResponse() })
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Obtener un usuario específico de un cliente")
    fun getUserById(
        @PathVariable tenantId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<UserResponse> {
        return findUseCase.findByIdAndTenantId(userId, TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.ok(it.toResponse()) }
        )
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario para un cliente")
    fun createUser(
        @PathVariable tenantId: Long,
        @Valid @RequestBody request: UserCreateRequest
    ): ResponseEntity<Any> {
        return createUseCase.execute(request.toCommand(TenantId(tenantId))).fold(
            onLeft = { error ->
                when (error) {
                    is UserError.DuplicateUsername ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is UserError.DuplicateEmail ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is UserError.InvalidRole ->
                        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapOf("error" to error.message))
                    is UserError.NotFound ->
                        ResponseEntity.notFound().build()
                }
            },
            onRight = { user ->
                ResponseEntity.status(HttpStatus.CREATED).body(user.toResponse())
            }
        )
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Actualizar un usuario de un cliente")
    fun updateUser(
        @PathVariable tenantId: Long,
        @PathVariable userId: Long,
        @Valid @RequestBody request: UserUpdateRequest
    ): ResponseEntity<Any> {
        return updateUseCase.execute(request.toCommand(userId, TenantId(tenantId))).fold(
            onLeft = { error ->
                when (error) {
                    is UserError.NotFound ->
                        ResponseEntity.notFound().build()
                    is UserError.DuplicateUsername ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is UserError.DuplicateEmail ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to error.message))
                    is UserError.InvalidRole ->
                        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapOf("error" to error.message))
                }
            },
            onRight = { user ->
                ResponseEntity.ok(user.toResponse())
            }
        )
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Eliminar un usuario de un cliente")
    fun deleteUser(
        @PathVariable tenantId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<Unit> {
        return deleteUseCase.execute(userId, TenantId(tenantId)).fold(
            onLeft = { ResponseEntity.notFound().build() },
            onRight = { ResponseEntity.noContent().build() }
        )
    }
}
