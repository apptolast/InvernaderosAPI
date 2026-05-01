package com.apptolast.invernaderos.features.auth.refresh.domain.port.input

import com.apptolast.invernaderos.features.auth.refresh.domain.model.AuthTokensResult
import com.apptolast.invernaderos.features.auth.refresh.domain.model.RefreshTokenFamilyId

data class IssueRefreshTokenCommand(
    val userId: Long,
    val familyId: RefreshTokenFamilyId? = null  // null → new family (login/register); set → rotation
)

interface IssueRefreshTokenUseCase {
    fun execute(cmd: IssueRefreshTokenCommand): AuthTokensResult
}
