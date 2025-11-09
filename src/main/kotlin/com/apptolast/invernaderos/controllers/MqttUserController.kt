package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.metadata.entity.MqttUsers
import com.apptolast.invernaderos.entities.metadata.entity.User
import com.apptolast.invernaderos.repositories.metadata.MqttUserRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MqttUserController(
    private val mqttUserRepository: MqttUserRepository
) {

    @GetMapping("/mqttusers")
    fun getAllUsers(): List<MqttUsers> {

        return mqttUserRepository.findAll()
    }
}