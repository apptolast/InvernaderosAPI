package com.apptolast.invernaderos.features.alert

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AlertStateChangeRepository : JpaRepository<AlertStateChange, Long>
