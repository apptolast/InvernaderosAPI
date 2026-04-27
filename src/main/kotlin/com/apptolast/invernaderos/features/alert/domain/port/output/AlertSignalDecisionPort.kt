package com.apptolast.invernaderos.features.alert.domain.port.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertMqttSignal
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalDecision

/**
 * Maps a raw MQTT signal + current alert state into a decision (ACTIVATE / RESOLVE / NO_OP).
 * The adapter implements this using `alert.mqtt.value-true-means` config.
 *
 * Why a port and not a domain function:
 * - Mapping is policy, not invariant. It can change at deploy-time via configmap.
 * - Keeping it as a port lets us swap the policy in tests without rebuilding.
 */
interface AlertSignalDecisionPort {
    fun decide(currentAlert: Alert, signal: AlertMqttSignal): AlertSignalDecision
}
