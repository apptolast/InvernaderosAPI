package com.apptolast.invernaderos.features.command.infrastructure.adapter.output

import com.apptolast.invernaderos.features.command.domain.port.output.CodeExistencePort
import com.apptolast.invernaderos.features.device.DeviceRepository
import com.apptolast.invernaderos.features.setting.SettingRepository
import org.springframework.stereotype.Component

@Component
class CodeExistenceAdapter(
    private val deviceRepository: DeviceRepository,
    private val settingRepository: SettingRepository
) : CodeExistencePort {

    override fun existsDeviceByCode(code: String): Boolean {
        return deviceRepository.findByCode(code) != null
    }

    override fun existsSettingByCode(code: String): Boolean {
        return settingRepository.findByCode(code) != null
    }
}
