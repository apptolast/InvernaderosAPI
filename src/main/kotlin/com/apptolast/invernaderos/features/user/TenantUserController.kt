package com.apptolast.invernaderos.features.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/users")
@Tag(name = "User Management", description = "Endpoints para la gestión de usuarios de un cliente")
class TenantUserController(
    private val userService: UserService
) {

    @GetMapping
    @Operation(summary = "Obtener todos los usuarios de un cliente")
    fun getAllUsers(@PathVariable tenantId: UUID): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(userService.findAllByTenantId(tenantId))
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Obtener un usuario específico de un cliente")
    fun getUserById(
        @PathVariable tenantId: UUID,
        @PathVariable userId: UUID
    ): ResponseEntity<UserResponse> {
        val user = userService.findByIdAndTenantId(userId, tenantId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario para un cliente")
    fun createUser(
        @PathVariable tenantId: UUID,
        @RequestBody request: UserCreateRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userService.createUser(tenantId, request))
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Actualizar un usuario de un cliente")
    fun updateUser(
        @PathVariable tenantId: UUID,
        @PathVariable userId: UUID,
        @RequestBody request: UserUpdateRequest
    ): ResponseEntity<UserResponse> {
        val updated = userService.updateUser(userId, tenantId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Eliminar un usuario de un cliente")
    fun deleteUser(
        @PathVariable tenantId: UUID,
        @PathVariable userId: UUID
    ): ResponseEntity<Unit> {
        return if (userService.deleteUser(userId, tenantId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
