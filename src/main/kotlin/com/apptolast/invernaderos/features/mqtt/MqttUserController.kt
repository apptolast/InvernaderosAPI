package com.apptolast.invernaderos.features.mqtt

import java.util.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mqttusers")
class MqttUserController(private val mqttUserService: MqttUserService) {

    @GetMapping
    fun getAllUsers(): List<MqttUserDto> {
        return mqttUserService.getAllUsers()
    }

    @GetMapping("/{id}")
    fun getMqttUser(@PathVariable id: UUID): ResponseEntity<MqttUserDto> {
        val user = mqttUserService.getMqttUser(id)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
