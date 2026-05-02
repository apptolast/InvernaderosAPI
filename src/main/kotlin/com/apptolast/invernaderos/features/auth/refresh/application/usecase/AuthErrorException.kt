package com.apptolast.invernaderos.features.auth.refresh.application.usecase

import com.apptolast.invernaderos.features.auth.refresh.domain.error.AuthError

class AuthErrorException(val error: AuthError) : RuntimeException(error.message)
