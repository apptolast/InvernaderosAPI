package com.apptolast.invernaderos.repositories.metadata

import com.apptolast.invernaderos.entities.metadata.entity.MqttUsers
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
interface MqttUserRepository : JpaRepository<MqttUsers, UUID> {
}