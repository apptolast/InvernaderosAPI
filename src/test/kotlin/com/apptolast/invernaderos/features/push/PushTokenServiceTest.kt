package com.apptolast.invernaderos.features.push

import com.apptolast.invernaderos.features.user.User
import com.apptolast.invernaderos.features.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.Instant

class PushTokenServiceTest {

    private lateinit var pushTokenRepository: PushTokenRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: PushTokenService

    @BeforeEach
    fun setup() {
        pushTokenRepository = mockk(relaxed = true)
        userRepository = mockk()
        service = PushTokenService(pushTokenRepository, userRepository)
    }

    private fun user(id: Long = 1L, tenantId: Long = 10L, email: String = "user@example.com") = User(
        id = id,
        code = "USR-00001",
        tenantId = tenantId,
        username = "user1",
        email = email,
        passwordHash = "hash",
        role = "USER",
        isActive = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `register inserts a new token when none exists for the value`() {
        every { userRepository.findByEmail("user@example.com") } returns user()
        every { pushTokenRepository.findByToken("tokA") } returns null
        val captured = slot<PushToken>()
        every { pushTokenRepository.save(capture(captured)) } answers { captured.captured.also { it.id = 99L } }

        val result = service.register("user@example.com", "tokA", PushPlatform.ANDROID)

        assertEquals(99L, result.id)
        assertEquals(1L, captured.captured.userId)
        assertEquals(10L, captured.captured.tenantId)
        assertEquals("tokA", captured.captured.token)
        assertEquals(PushPlatform.ANDROID, captured.captured.platform)
    }

    @Test
    fun `register updates existing row when token already present (upsert)`() {
        val existing = PushToken(
            id = 7L,
            userId = 99L,            // OLD owner
            tenantId = 999L,         // OLD tenant
            token = "tokA",
            platform = PushPlatform.IOS,
            lastSeenAt = Instant.parse("2020-01-01T00:00:00Z")
        )
        every { userRepository.findByEmail("user@example.com") } returns user()
        every { pushTokenRepository.findByToken("tokA") } returns existing
        every { pushTokenRepository.save(any<PushToken>()) } answers { firstArg() }

        val result = service.register("user@example.com", "tokA", PushPlatform.ANDROID)

        assertEquals(7L, result.id)
        // Upserted to new owner / tenant / platform.
        assertEquals(1L, result.userId)
        assertEquals(10L, result.tenantId)
        assertEquals(PushPlatform.ANDROID, result.platform)
        // last_seen_at refreshed.
        assert(result.lastSeenAt.isAfter(Instant.parse("2020-01-01T00:00:00Z")))
        verify(exactly = 1) { pushTokenRepository.save(existing) }
    }

    @Test
    fun `register falls back to findByUsername when email lookup fails`() {
        every { userRepository.findByEmail("user1") } returns null
        every { userRepository.findByUsername("user1") } returns user(email = "u1@x.com")
        every { pushTokenRepository.findByToken("t") } returns null
        every { pushTokenRepository.save(any<PushToken>()) } answers { firstArg() }

        service.register("user1", "t", PushPlatform.WEB)

        verify { userRepository.findByEmail("user1") }
        verify { userRepository.findByUsername("user1") }
    }

    @Test
    fun `register throws when authenticated user is not in the DB`() {
        every { userRepository.findByEmail("ghost") } returns null
        every { userRepository.findByUsername("ghost") } returns null

        assertThrows(UsernameNotFoundException::class.java) {
            service.register("ghost", "tok", PushPlatform.ANDROID)
        }
    }

    @Test
    fun `unregister deletes by token and returns true on hit`() {
        every { pushTokenRepository.deleteByToken("tokA") } returns 1

        assertEquals(true, service.unregister("tokA"))
    }

    @Test
    fun `unregister returns false on miss`() {
        every { pushTokenRepository.deleteByToken("missing") } returns 0

        assertEquals(false, service.unregister("missing"))
    }
}
