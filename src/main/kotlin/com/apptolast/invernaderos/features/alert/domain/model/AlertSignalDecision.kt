package com.apptolast.invernaderos.features.alert.domain.model

/**
 * Outcome of evaluating an MQTT signal against the current Alert state.
 * - ACTIVATE: the alert must transition to is_resolved=false
 * - RESOLVE:  the alert must transition to is_resolved=true
 * - NO_OP:    the signal does not require a state change (already in target state)
 */
enum class AlertSignalDecision { ACTIVATE, RESOLVE, NO_OP }
