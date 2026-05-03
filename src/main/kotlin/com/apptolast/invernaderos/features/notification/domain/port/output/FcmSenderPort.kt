package com.apptolast.invernaderos.features.notification.domain.port.output

import com.apptolast.invernaderos.features.notification.domain.model.NotificationContent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationRecipient

/**
 * Driven port for sending push notifications via FCM.
 *
 * [send] dispatches [content] to every token in [recipients] and returns a [FcmSendResult]
 * summarising successes, failures, and permanently-invalidated token IDs.
 */
interface FcmSenderPort {
    fun send(recipients: List<NotificationRecipient>, content: NotificationContent): FcmSendResult
}

/**
 * Outcome of a single [FcmSenderPort.send] call.
 *
 * [invalidatedTokens] holds the [NotificationRecipient.tokenId] values whose FCM tokens
 * were rejected with UNREGISTERED or INVALID_ARGUMENT and have been (or must be) deleted.
 * [errors] maps tokenId to a human-readable error description for non-invalidating failures.
 */
data class FcmSendResult(
    val success: Int,
    val failed: Int,
    val invalidatedTokens: List<Long>,
    val errors: Map<Long, String>
)
