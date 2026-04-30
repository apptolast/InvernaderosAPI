package com.apptolast.invernaderos.features.push

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.SendResponse
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests for FcmPushService — MockK style, no Spring context.
 */
class FcmPushServiceTest {

    private lateinit var firebaseMessaging: FirebaseMessaging
    private lateinit var pushTokenRepository: PushTokenRepository
    private lateinit var meterRegistry: MeterRegistry
    private lateinit var service: FcmPushService

    @BeforeEach
    fun setup() {
        firebaseMessaging = mockk()
        pushTokenRepository = mockk(relaxed = true)
        meterRegistry = SimpleMeterRegistry()
        service = FcmPushService(firebaseMessaging, pushTokenRepository, meterRegistry)
    }

    private fun samplePayload(tenantId: Long = 10L) = AlertPushPayload(
        alertId = 1L,
        alertCode = "ALT-00001",
        tenantId = tenantId,
        greenhouseId = 100L,
        sectorId = 20L,
        severityName = "CRITICAL",
        severityLevel = 4,
        severityColor = "#FF0000",
        title = "Nueva alerta: CRITICAL",
        body = "Sensor offline",
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    private fun pushToken(token: String, id: Long = 1L) = PushToken(
        id = id,
        userId = 1L,
        tenantId = 10L,
        token = token,
        platform = PushPlatform.ANDROID
    )

    @Test
    fun `should skip multicast when no tokens are registered for tenant`() {
        every { pushTokenRepository.findAllByTenantId(10L) } returns emptyList()

        service.sendAlertToTenant(samplePayload())

        verify(exactly = 0) { firebaseMessaging.sendEachForMulticast(any()) }
    }

    @Test
    fun `should send single multicast message when tokens fit in one batch`() {
        every { pushTokenRepository.findAllByTenantId(10L) } returns listOf(
            pushToken("tokA"),
            pushToken("tokB", id = 2L),
            pushToken("tokC", id = 3L)
        )
        val response: BatchResponse = mockk {
            every { successCount } returns 3
            every { failureCount } returns 0
            every { responses } returns listOf(successResponse(), successResponse(), successResponse())
        }
        val captured = slot<MulticastMessage>()
        every { firebaseMessaging.sendEachForMulticast(capture(captured)) } returns response

        service.sendAlertToTenant(samplePayload())

        verify(exactly = 1) { firebaseMessaging.sendEachForMulticast(any()) }
    }

    @Test
    fun `should chunk multicast in batches of 500`() {
        val tokens = (1..1234).map { pushToken("t$it", id = it.toLong()) }
        every { pushTokenRepository.findAllByTenantId(10L) } returns tokens
        val ok: BatchResponse = mockk {
            every { successCount } returns 500
            every { failureCount } returns 0
            every { responses } returns List(500) { successResponse() }
        }
        every { firebaseMessaging.sendEachForMulticast(any()) } returns ok

        service.sendAlertToTenant(samplePayload())

        // 500 + 500 + 234 = 1234 → 3 multicast calls
        verify(exactly = 3) { firebaseMessaging.sendEachForMulticast(any()) }
    }

    @Test
    fun `should delete token returning UNREGISTERED error`() {
        every { pushTokenRepository.findAllByTenantId(10L) } returns listOf(
            pushToken("dead"), pushToken("alive", id = 2L)
        )
        every { pushTokenRepository.deleteByToken("dead") } returns 1
        val unreg = unregisteredResponse()
        val ok = successResponse()
        val response: BatchResponse = mockk {
            every { successCount } returns 1
            every { failureCount } returns 1
            every { responses } returns listOf(unreg, ok)
        }
        every { firebaseMessaging.sendEachForMulticast(any()) } returns response

        service.sendAlertToTenant(samplePayload())

        verify(exactly = 1) { pushTokenRepository.deleteByToken("dead") }
        verify(exactly = 0) { pushTokenRepository.deleteByToken("alive") }
    }

    @Test
    fun `should delete token returning INVALID_ARGUMENT error`() {
        every { pushTokenRepository.findAllByTenantId(10L) } returns listOf(pushToken("garbage"))
        every { pushTokenRepository.deleteByToken("garbage") } returns 1
        val invalid = invalidArgumentResponse()
        val response: BatchResponse = mockk {
            every { successCount } returns 0
            every { failureCount } returns 1
            every { responses } returns listOf(invalid)
        }
        every { firebaseMessaging.sendEachForMulticast(any()) } returns response

        service.sendAlertToTenant(samplePayload())

        verify(exactly = 1) { pushTokenRepository.deleteByToken("garbage") }
    }

    @Test
    fun `should NOT delete token for transient errors such as INTERNAL`() {
        every { pushTokenRepository.findAllByTenantId(10L) } returns listOf(pushToken("transient"))
        val transient = errorResponse(MessagingErrorCode.INTERNAL)
        val response: BatchResponse = mockk {
            every { successCount } returns 0
            every { failureCount } returns 1
            every { responses } returns listOf(transient)
        }
        every { firebaseMessaging.sendEachForMulticast(any()) } returns response

        service.sendAlertToTenant(samplePayload())

        verify(exactly = 0) { pushTokenRepository.deleteByToken(any()) }
    }

    @Test
    fun `should be a no-op when FirebaseMessaging is null (graceful degradation)`() {
        val degraded = FcmPushService(null, pushTokenRepository, meterRegistry)
        degraded.sendAlertToTenant(samplePayload())
        verify(exactly = 0) { pushTokenRepository.findAllByTenantId(any()) }
    }

    @Test
    fun `should swallow exception thrown by FCM and continue`() {
        every { pushTokenRepository.findAllByTenantId(10L) } returns listOf(pushToken("x"))
        every { firebaseMessaging.sendEachForMulticast(any()) } throws RuntimeException("FCM down")

        // Must not throw
        service.sendAlertToTenant(samplePayload())

        verify(exactly = 0) { pushTokenRepository.deleteByToken(any()) }
    }

    @Test
    fun `failure metric is incremented per failed token`() {
        every { pushTokenRepository.findAllByTenantId(10L) } returns listOf(
            pushToken("a"), pushToken("b", id = 2L)
        )
        val response: BatchResponse = mockk {
            every { successCount } returns 0
            every { failureCount } returns 2
            every { responses } returns listOf(unregisteredResponse(), invalidArgumentResponse())
        }
        every { firebaseMessaging.sendEachForMulticast(any()) } returns response

        service.sendAlertToTenant(samplePayload())

        val unregMetric = meterRegistry.find("push.fcm.failed").tag("reason", "UNREGISTERED").counter()
        val invalidMetric = meterRegistry.find("push.fcm.failed").tag("reason", "INVALID_ARGUMENT").counter()
        assertEquals(1.0, unregMetric?.count())
        assertEquals(1.0, invalidMetric?.count())
    }

    // -------------------------------------------------------------------------
    // Helpers — SendResponse mocks
    // -------------------------------------------------------------------------

    private fun successResponse(): SendResponse = mockk {
        every { isSuccessful } returns true
    }

    private fun unregisteredResponse(): SendResponse {
        val ex = mockk<FirebaseMessagingException> {
            every { messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
        }
        return mockk {
            every { isSuccessful } returns false
            every { exception } returns ex
        }
    }

    private fun invalidArgumentResponse(): SendResponse {
        val ex = mockk<FirebaseMessagingException> {
            every { messagingErrorCode } returns MessagingErrorCode.INVALID_ARGUMENT
        }
        return mockk {
            every { isSuccessful } returns false
            every { exception } returns ex
        }
    }

    private fun errorResponse(code: MessagingErrorCode): SendResponse {
        val ex = mockk<FirebaseMessagingException> {
            every { messagingErrorCode } returns code
        }
        return mockk {
            every { isSuccessful } returns false
            every { exception } returns ex
        }
    }
}
