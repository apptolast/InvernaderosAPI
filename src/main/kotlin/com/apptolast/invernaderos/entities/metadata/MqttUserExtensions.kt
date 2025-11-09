package com.apptolast.invernaderos.entities.metadata

import com.apptolast.invernaderos.entities.metadata.dto.MqttUserDto
import com.apptolast.invernaderos.entities.metadata.entity.MqttUsers

fun MqttUsers.toDto(): MqttUserDto = MqttUserDto(
    username = this.username,
    password = this.passwordHash,
    salt = this.salt,
    deviceType = this.deviceType,
    isActive = this.isActive
)

fun List<MqttUsers>.toDtoList() = this.map { it.toDto() }