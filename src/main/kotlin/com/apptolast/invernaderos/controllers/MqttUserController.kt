package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.metadata.dto.MqttUserDto
import com.apptolast.invernaderos.entities.metadata.toDto
import com.apptolast.invernaderos.entities.metadata.toDtoList
import com.apptolast.invernaderos.repositories.metadata.MqttUserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/mqttusers")
class MqttUserController(
    private val mqttUserRepository: MqttUserRepository
) {

    @GetMapping
    fun getAllUsers(): List<MqttUserDto> {
        return mqttUserRepository.findAll().toDtoList()
    }

    @GetMapping("/{id}")
    fun getMqttUser(@PathVariable id: UUID): ResponseEntity<MqttUserDto> {
        return mqttUserRepository.findById(id)
            .map { user -> ResponseEntity.ok(user.toDto()) }
            .orElse(ResponseEntity.notFound().build())
    }
}