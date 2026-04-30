package com.apptolast.invernaderos.features.push

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Envía notificaciones push FCM a todos los tokens registrados de un tenant.
 *
 * Trocea el envío en grupos de [FCM_MULTICAST_BATCH] (máximo permitido por
 * `sendEachForMulticast` según la documentación de FCM HTTP v1) para no
 * sobrepasar el límite del SDK.
 *
 * Limpieza automática de tokens muertos: cuando FCM responde con
 * `UNREGISTERED` (la app fue desinstalada) o `INVALID_ARGUMENT` (token
 * malformado o ya rotado), se borra la fila correspondiente para que el
 * próximo envío no la incluya.
 *
 * Si no hay `FirebaseMessaging` (caso "sin credenciales", ver
 * [com.apptolast.invernaderos.features.push.infrastructure.config.FirebaseConfig]),
 * el envío es un no-op silencioso. Esto permite que el sistema siga funcional
 * para entornos sin Firebase configurado.
 */
@Service
class FcmPushService(
    @Autowired(required = false)
    private val firebaseMessaging: FirebaseMessaging?,
    private val pushTokenRepository: PushTokenRepository,
    private val meterRegistry: MeterRegistry
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional("metadataTransactionManager")
    fun sendAlertToTenant(payload: AlertPushPayload) {
        val messaging = firebaseMessaging ?: run {
            logger.debug(
                "FCM disabled (no FirebaseMessaging bean) — skipping push for alert {}",
                payload.alertCode
            )
            return
        }

        val tokens = pushTokenRepository.findAllByTenantId(payload.tenantId).map { it.token }
        if (tokens.isEmpty()) {
            logger.info(
                "No push tokens registered for tenantId={} — skipping push for alert {}",
                payload.tenantId, payload.alertCode
            )
            return
        }

        var totalSuccess = 0
        var totalFailed = 0
        tokens.chunked(FCM_MULTICAST_BATCH).forEach { chunk ->
            val message = buildMulticastMessage(chunk, payload)
            try {
                val response = messaging.sendEachForMulticast(message)
                totalSuccess += response.successCount
                totalFailed += response.failureCount

                response.responses.forEachIndexed { index, sendResponse ->
                    if (!sendResponse.isSuccessful) {
                        val code = sendResponse.exception?.messagingErrorCode
                        val deadToken = chunk[index]
                        if (code == MessagingErrorCode.UNREGISTERED ||
                            code == MessagingErrorCode.INVALID_ARGUMENT) {
                            val deletedRows = pushTokenRepository.deleteByToken(deadToken)
                            logger.info(
                                "Removed invalid FCM token (code={}) deletedRows={} alertCode={}",
                                code, deletedRows, payload.alertCode
                            )
                        } else {
                            logger.warn(
                                "FCM send failure (code={}) tokenSuffix=...{} alertCode={}",
                                code,
                                deadToken.takeLast(6),
                                payload.alertCode,
                                sendResponse.exception
                            )
                        }
                        meterRegistry.counter(
                            "push.fcm.failed",
                            "reason", code?.name ?: "UNKNOWN"
                        ).increment()
                    }
                }
            } catch (ex: Exception) {
                logger.error(
                    "FCM multicast call failed for alertCode={} chunkSize={}",
                    payload.alertCode, chunk.size, ex
                )
                totalFailed += chunk.size
                meterRegistry.counter("push.fcm.failed", "reason", "EXCEPTION").increment()
            }
        }

        meterRegistry.counter("push.fcm.sent").increment(totalSuccess.toDouble())
        logger.info(
            "FCM push sent: tenantId={} alertCode={} tokens={} success={} failed={}",
            payload.tenantId, payload.alertCode, tokens.size, totalSuccess, totalFailed
        )
    }

    private fun buildMulticastMessage(
        tokens: List<String>,
        payload: AlertPushPayload
    ): MulticastMessage {
        val builder = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(
                Notification.builder()
                    .setTitle(payload.title)
                    .setBody(payload.body)
                    .build()
            )
            .putData("alertId", payload.alertId.toString())
            .putData("alertCode", payload.alertCode)
            .putData("greenhouseId", payload.greenhouseId.toString())
            .putData("sectorId", payload.sectorId.toString())
            .putData("severity", payload.severityName)
            .putData("severityLevel", payload.severityLevel.toString())
            .putData("createdAt", payload.createdAt.toEpochMilli().toString())
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification.builder()
                            .setChannelId(ANDROID_CHANNEL_ID)
                            .setColor(payload.severityColor ?: DEFAULT_COLOR)
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
        return builder.build()
    }

    companion object {
        /**
         * Máximo de tokens por llamada `sendEachForMulticast` (FCM HTTP v1).
         */
        const val FCM_MULTICAST_BATCH = 500

        /**
         * Canal de notificación Android. El cliente debe crear este channel
         * antes de mostrar la primera notificación; ver `GreenhouseFcmService`
         * en el frontend KMP.
         */
        const val ANDROID_CHANNEL_ID = "alerts_default"

        private const val DEFAULT_COLOR = "#00E676"
    }
}
