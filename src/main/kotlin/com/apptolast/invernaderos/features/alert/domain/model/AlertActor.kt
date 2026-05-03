package com.apptolast.invernaderos.features.alert.domain.model

sealed interface AlertActor {
    data class User(val userId: Long, val username: String?, val displayName: String?) : AlertActor
    data class Device(val deviceRef: String?) : AlertActor
    data object System : AlertActor
}
