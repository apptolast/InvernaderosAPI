package com.apptolast.invernaderos.features.mqtt

import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MqttUserRepository : JpaRepository<MqttUsers, UUID> {
    @EntityGraph(value = "MqttUsers.context") fun findByUsername(username: String): MqttUsers?
}
