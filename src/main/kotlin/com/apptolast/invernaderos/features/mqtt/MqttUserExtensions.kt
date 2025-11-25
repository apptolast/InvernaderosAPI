package com.apptolast.invernaderos.features.mqtt

import com.apptolast.invernaderos.features.mqtt.MqttUserDto
import com.apptolast.invernaderos.features.mqtt.MqttUsers

fun MqttUsers.toDto(): MqttUserDto = MqttUserDto(
    username = this.username,
    password = this.passwordHash,
    salt = this.salt,
    deviceType = this.deviceType,
    isActive = this.isActive
)

fun List<MqttUsers>.toDtoList() = this.map { it.toDto() }