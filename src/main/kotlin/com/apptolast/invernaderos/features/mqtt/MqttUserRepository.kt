package com.apptolast.invernaderos.features.mqtt

import com.apptolast.invernaderos.features.mqtt.MqttUsers
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
interface MqttUserRepository : JpaRepository<MqttUsers, UUID> {
}