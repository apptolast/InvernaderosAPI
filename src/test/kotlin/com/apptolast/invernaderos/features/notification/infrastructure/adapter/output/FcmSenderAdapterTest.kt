package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.domain.model.NotificationContent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationRecipient
import com.apptolast.invernaderos.features.push.PushTokenRepository
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.retry.support.RetryTemplate
import java.util.Locale

class FcmSenderAdapterTest {

    private val firebaseMessaging = mockk<FirebaseMessaging>()
    private val pushTokenRepository = mockk<PushTokenRepository>(relaxed = true)
    private val meterRegistry = SimpleMeterRegistry()
    private val adapter = FcmSenderAdapter(
        firebaseMessaging = firebaseMessaging,
        pushTokenRepository = pushTokenRepository,
        meterRegistry = meterRegistry,
        fcmRetryTemplate = RetryTemplate()
    )

    @Test
    fun `send captures FCM message ids by token id for successful responses`() {
        every { firebaseMessaging.sendEachForMulticast(any()) } returns batch(
            success("projects/demo/messages/100"),
            success("projects/demo/messages/101")
        )

        val result = adapter.send(
            recipients = listOf(
                recipient(tokenId = 100L, tokenValue = "token-a"),
                recipient(tokenId = 101L, tokenValue = "token-b"),
            ),
            content = content()
        )

        assertThat(result.success).isEqualTo(2)
        assertThat(result.failed).isZero()
        assertThat(result.invalidatedTokens).isEmpty()
        assertThat(result.messageIdsByTokenId).containsEntry(100L, "projects/demo/messages/100")
        assertThat(result.messageIdsByTokenId).containsEntry(101L, "projects/demo/messages/101")
    }

    @Test
    fun `send invalidates dead tokens without counting them as failed`() {
        every { pushTokenRepository.deleteByToken("dead-token") } returns 1
        every { firebaseMessaging.sendEachForMulticast(any()) } returns batch(
            invalid(MessagingErrorCode.UNREGISTERED),
            success("projects/demo/messages/alive")
        )

        val result = adapter.send(
            recipients = listOf(
                recipient(tokenId = 100L, tokenValue = "dead-token"),
                recipient(tokenId = 101L, tokenValue = "alive-token"),
            ),
            content = content()
        )

        assertThat(result.success).isEqualTo(1)
        assertThat(result.failed).isZero()
        assertThat(result.invalidatedTokens).containsExactly(100L)
        assertThat(result.errors).isEmpty()
        assertThat(result.messageIdsByTokenId).containsEntry(101L, "projects/demo/messages/alive")
        verify(exactly = 1) { pushTokenRepository.deleteByToken("dead-token") }
    }

    @Test
    fun `send counts non-invalidating FCM errors as failed`() {
        every { firebaseMessaging.sendEachForMulticast(any()) } returns batch(
            invalid(MessagingErrorCode.INTERNAL)
        )

        val result = adapter.send(listOf(recipient(tokenId = 100L, tokenValue = "token")), content())

        assertThat(result.success).isZero()
        assertThat(result.failed).isEqualTo(1)
        assertThat(result.invalidatedTokens).isEmpty()
        assertThat(result.errors).containsKey(100L)
        verify(exactly = 0) { pushTokenRepository.deleteByToken(any()) }
    }

    @Test
    fun `send retains INVALID_ARGUMENT tokens and counts them as failed`() {
        every { firebaseMessaging.sendEachForMulticast(any()) } returns batch(
            invalid(MessagingErrorCode.INVALID_ARGUMENT)
        )

        val result = adapter.send(listOf(recipient(tokenId = 100L, tokenValue = "token")), content())

        assertThat(result.success).isZero()
        assertThat(result.failed).isEqualTo(1)
        assertThat(result.invalidatedTokens).isEmpty()
        assertThat(result.errors).containsKey(100L)
        verify(exactly = 0) { pushTokenRepository.deleteByToken(any()) }
    }

    @Test
    fun `send invalidates SENDER_ID_MISMATCH tokens`() {
        every { pushTokenRepository.deleteByToken("wrong-project-token") } returns 1
        every { firebaseMessaging.sendEachForMulticast(any()) } returns batch(
            invalid(MessagingErrorCode.SENDER_ID_MISMATCH)
        )

        val result = adapter.send(listOf(recipient(tokenId = 100L, tokenValue = "wrong-project-token")), content())

        assertThat(result.success).isZero()
        assertThat(result.failed).isZero()
        assertThat(result.invalidatedTokens).containsExactly(100L)
        assertThat(result.errors).isEmpty()
        verify(exactly = 1) { pushTokenRepository.deleteByToken("wrong-project-token") }
    }

    private fun recipient(tokenId: Long, tokenValue: String) = NotificationRecipient(
        userId = tokenId + 1,
        tokenId = tokenId,
        tokenValue = tokenValue,
        locale = Locale.forLanguageTag("es-ES")
    )

    private fun content() = NotificationContent(
        title = "title",
        body = "body",
        data = mapOf("alertId" to "1"),
        androidChannelId = "alerts_default",
        severityColor = "#FF0000"
    )

    private fun batch(vararg responses: SendResponse): BatchResponse = mockk {
        every { this@mockk.responses } returns responses.toList()
    }

    private fun success(messageId: String): SendResponse = mockk {
        every { isSuccessful } returns true
        every { this@mockk.messageId } returns messageId
    }

    private fun invalid(code: MessagingErrorCode): SendResponse {
        val ex = mockk<FirebaseMessagingException> {
            every { messagingErrorCode } returns code
            every { message } returns code.name
        }
        return mockk {
            every { isSuccessful } returns false
            every { exception } returns ex
        }
    }
}
