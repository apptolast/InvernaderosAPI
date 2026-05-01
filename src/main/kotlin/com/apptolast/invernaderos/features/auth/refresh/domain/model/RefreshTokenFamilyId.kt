package com.apptolast.invernaderos.features.auth.refresh.domain.model

import java.util.UUID

@JvmInline value class RefreshTokenFamilyId(val value: UUID) {
    companion object {
        fun new(): RefreshTokenFamilyId = RefreshTokenFamilyId(UUID.randomUUID())
    }
}
