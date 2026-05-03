package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.domain.model.NotificationContent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationRecipient
import com.apptolast.invernaderos.features.notification.domain.port.output.FcmSendResult
import com.apptolast.invernaderos.features.notification.domain.port.output.FcmSenderPort
import com.apptolast.invernaderos.features.push.PushTokenRepository
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component

private const val DEFAULT_COLOR = "#00E676"
private const val FCM_BATCH_SIZE = 500

@Component
class FcmSenderAdapter(
    @Autowired(required = false)
    private val firebaseMessaging: FirebaseMessaging?,
    private val pushTokenRepository: PushTokenRepository,
    private val meterRegistry: MeterRegistry,
    private val fcmRetryTemplate: RetryTemplate
) : FcmSenderPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(
        recipients: List<NotificationRecipient>,
        content: NotificationContent
    ): FcmSendResult {
        if (firebaseMessaging == null) {
            logger.info(
                "FCM disabled (no FirebaseMessaging bean) — skipping dispatch for {} recipients",
                recipients.size
            )
            return FcmSendResult(
                success = 0,
                failed = recipients.size,
                invalidatedTokens = emptyList(),
                errors = recipients.associate { it.tokenId to "FCM disabled" }
            )
        }

        val sample = Timer.start(meterRegistry)
        var totalSuccess = 0
        var totalFailed = 0
        val invalidatedTokenIds = mutableListOf<Long>()
        val errors = mutableMapOf<Long, String>()

        recipients.chunked(FCM_BATCH_SIZE).forEach { chunk ->
            val tokens = chunk.map { it.tokenValue }
            val message = buildMulticastMessage(tokens, content)

            val response: BatchResponse = try {
                fcmRetryTemplate.execute<BatchResponse, Exception> {
                    firebaseMessaging.sendEachForMulticast(message)
                }
            } catch (ex: Exception) {
                logger.error(
                    "FCM multicast call failed after retries for chunkSize={}: {}",
                    chunk.size, ex.message, ex
                )
                totalFailed += chunk.size
                chunk.forEach { recipient ->
                    errors[recipient.tokenId] = "FCM call failed: ${ex.message}"
                }
                meterRegistry.counter("notification.dispatched", "result", "FAILURE")
                    .increment(chunk.size.toDouble())
                return@forEach
            }

            response.responses.forEachIndexed { index, sendResponse ->
                val recipient = chunk[index]
                if (sendResponse.isSuccessful) {
                    totalSuccess++
                    meterRegistry.counter("notification.dispatched", "result", "SUCCESS").increment()
                } else {
                    val code = sendResponse.exception?.messagingErrorCode
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        val deletedRows = pushTokenRepository.deleteByToken(recipient.tokenValue)
                        logger.info(
                            "Removed invalid FCM token code={} deletedRows={} tokenId={}",
                            code, deletedRows, recipient.tokenId
                        )
                        invalidatedTokenIds.add(recipient.tokenId)
                        meterRegistry.counter(
                            "notification.fcm.token.invalidated",
                            "reason", code.name
                        ).increment()
                    } else {
                        val errorMsg = "FCM error code=${code?.name ?: "UNKNOWN"}: ${sendResponse.exception?.message}"
                        errors[recipient.tokenId] = errorMsg
                        logger.warn(
                            "FCM send failure code={} tokenId={}: {}",
                            code?.name, recipient.tokenId, sendResponse.exception?.message
                        )
                    }
                    totalFailed++
                    meterRegistry.counter("notification.dispatched", "result", "FAILURE").increment()
                }
            }
        }

        sample.stop(meterRegistry.timer("notification.fcm.duration"))

        logger.info(
            "FCM dispatch complete: recipients={} success={} failed={} invalidated={}",
            recipients.size, totalSuccess, totalFailed, invalidatedTokenIds.size
        )

        return FcmSendResult(
            success = totalSuccess,
            failed = totalFailed,
            invalidatedTokens = invalidatedTokenIds,
            errors = errors
        )
    }

    private fun buildMulticastMessage(tokens: List<String>, content: NotificationContent): MulticastMessage {
        val builder = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(
                Notification.builder()
                    .setTitle(content.title)
                    .setBody(content.body)
                    .build()
            )
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification.builder()
                            .setChannelId(content.androidChannelId)
                            .setColor(content.severityColor ?: DEFAULT_COLOR)
                            .build()
                    )
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound("default")
                            .setContentAvailable(true)
                            .build()
                    )
                    .build()
            )

        content.data.forEach { (k, v) -> builder.putData(k, v) }

        return builder.build()
    }
}
