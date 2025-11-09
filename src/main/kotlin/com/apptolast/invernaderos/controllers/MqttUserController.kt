package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.metadata.entity.MqttUsers
import com.apptolast.invernaderos.entities.metadata.entity.User
import com.apptolast.invernaderos.repositories.metadata.MqttUserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Optional
import java.util.UUID

@RestController
@RequestMapping("/mqttusers")
class MqttUserController(
    private val mqttUserRepository: MqttUserRepository
) {

    @GetMapping
    fun getAllUsers(): List<MqttUsers> {
        return mqttUserRepository.findAll()
    }

    @GetMapping("/{id}")
    fun getMqttUser(@PathVariable id: UUID): ResponseEntity<MqttUsers> {
        return mqttUserRepository.findById(id)
            .map { user -> ResponseEntity.ok(user) }
            .orElse(ResponseEntity.notFound().build())
    }
}