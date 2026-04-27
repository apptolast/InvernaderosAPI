package com.apptolast.invernaderos.features.alert.infrastructure.adapter.input

import com.apptolast.invernaderos.features.alert.domain.model.AlertMqttSignal
import com.apptolast.invernaderos.features.alert.domain.port.input.ApplyAlertMqttSignalUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Input adapter: bridges the MQTT transport layer into the alert domain use case.
 *
 * The @Transactional boundary lives here (rather than on the use case) so the
 * plain-Kotlin use case stays framework-free while the two writes — alert update
 * and state change persistence — are still atomic within a single metadata transaction.
 */
@Component
class AlertMqttInboundAdapter(
    private val applyUseCase: ApplyAlertMqttSignalUseCase
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Handles an ALT- prefixed MQTT signal. Never throws: any exception is caught and
     * logged so that a downstream alert misconfiguration cannot break telemetry ingestion.
     */
    @Transactional("metadataTransactionManager", rollbackFor = [Exception::class])
    fun handleSignal(code: String, rawValue: String) {
        try {
            val signal = AlertMqttSignal(code = code, rawValue = rawValue)
            applyUseCase.execute(signal).fold(
                onLeft = { error ->
                    // UnknownCode, NoTransitionRequired and InvalidSignalValue are
                    // already logged inside the use case — nothing more needed here.
                    logger.debug("Alert signal for '{}' resulted in left: {}", code, error.message)
                },
                onRight = { applied ->
                    logger.info(
                        "Alert {} transitioned: from_resolved={}, to_resolved={}",
                        applied.alert.code,
                        applied.change?.fromResolved,
                        applied.change?.toResolved
                    )
                }
            )
        } catch (ex: Exception) {
            logger.error(
                "Unexpected error processing MQTT alert signal for code='{}', value='{}' — telemetry flow is unaffected",
                code, rawValue, ex
            )
        }
    }
}
