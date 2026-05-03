package com.apptolast.invernaderos.features.notification

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserNotificationPreferencesJpaRepository : JpaRepository<UserNotificationPreferencesEntity, Long>
