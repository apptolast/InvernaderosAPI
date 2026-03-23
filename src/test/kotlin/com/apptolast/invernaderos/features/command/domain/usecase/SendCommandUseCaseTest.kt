package com.apptolast.invernaderos.features.command.domain.usecase

import com.apptolast.invernaderos.features.command.application.usecase.SendCommandUseCaseImpl
import com.apptolast.invernaderos.features.command.domain.error.CommandError
import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import com.apptolast.invernaderos.features.command.domain.port.input.SendDeviceCommand
import com.apptolast.invernaderos.features.command.domain.port.output.CodeExistencePort
import com.apptolast.invernaderos.features.command.domain.port.output.CommandAuditPersistencePort
import com.apptolast.invernaderos.features.command.domain.port.output.CommandPublisherPort
import com.apptolast.invernaderos.features.command.domain.port.output.DeviceCommandPersistencePort
import com.apptolast.invernaderos.features.command.domain.port.output.UserInfo
import com.apptolast.invernaderos.features.command.domain.port.output.UserLookupPort
import com.apptolast.invernaderos.features.shared.domain.Either
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SendCommandUseCaseTest {

    private val persistence = mockk<DeviceCommandPersistencePort>()
    private val codeExistence = mockk<CodeExistencePort>()
    private val publisher = mockk<CommandPublisherPort>()
    private val userLookup = mockk<UserLookupPort>()
    private val auditPersistence = mockk<CommandAuditPersistencePort>()
    private val useCase = SendCommandUseCaseImpl(persistence, codeExistence, publisher, userLookup, auditPersistence)

    @Test
    fun `should send command when code exists as device`() {
        val command = SendDeviceCommand(code = "DEV-00001", value = "22", email = "test@example.com")

        every { codeExistence.existsDeviceByCode("DEV-00001") } returns true
        every { codeExistence.existsSettingByCode("DEV-00001") } returns false
        every { userLookup.findUserByEmail("test@example.com") } returns UserInfo(userId = 1L, tenantId = 1L)
        every { persistence.save(any()) } answers { firstArg() }
        justRun { publisher.publish("DEV-00001", "22") }
        justRun { auditPersistence.save(any(), any(), any(), any(), any()) }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val saved = (result as Either.Right).value
        assertThat(saved.code).isEqualTo("DEV-00001")
        assertThat(saved.value).isEqualTo("22")
        verify(exactly = 1) { persistence.save(any()) }
        verify(exactly = 1) { publisher.publish("DEV-00001", "22") }
        verify(exactly = 1) { auditPersistence.save(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should send command when code exists as setting`() {
        val command = SendDeviceCommand(code = "SET-00036", value = "15", email = "test@example.com")

        every { codeExistence.existsDeviceByCode("SET-00036") } returns false
        every { codeExistence.existsSettingByCode("SET-00036") } returns true
        every { userLookup.findUserByEmail("test@example.com") } returns UserInfo(userId = 1L, tenantId = 1L)
        every { persistence.save(any()) } answers { firstArg() }
        justRun { publisher.publish("SET-00036", "15") }
        justRun { auditPersistence.save(any(), any(), any(), any(), any()) }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { publisher.publish("SET-00036", "15") }
    }

    @Test
    fun `should return CodeNotFound when code does not exist`() {
        val command = SendDeviceCommand(code = "UNKNOWN-99", value = "0", email = "test@example.com")

        every { codeExistence.existsDeviceByCode("UNKNOWN-99") } returns false
        every { codeExistence.existsSettingByCode("UNKNOWN-99") } returns false

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(CommandError.CodeNotFound::class.java)
        verify(exactly = 0) { persistence.save(any()) }
        verify(exactly = 0) { publisher.publish(any(), any()) }
    }

    @Test
    fun `should return UserNotFound when email does not exist`() {
        val command = SendDeviceCommand(code = "DEV-00001", value = "22", email = "unknown@example.com")

        every { codeExistence.existsDeviceByCode("DEV-00001") } returns true
        every { codeExistence.existsSettingByCode("DEV-00001") } returns false
        every { userLookup.findUserByEmail("unknown@example.com") } returns null

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(CommandError.UserNotFound::class.java)
        verify(exactly = 0) { persistence.save(any()) }
    }
}
