package com.apptolast.invernaderos.features.command.domain.port.output

interface CodeExistencePort {
    fun existsDeviceByCode(code: String): Boolean
    fun existsSettingByCode(code: String): Boolean
}
