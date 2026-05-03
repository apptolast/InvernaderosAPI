package com.apptolast.invernaderos.features.notification.domain.model

/**
 * The delivery channel preferred by the user. Only [PUSH] is delivered in the current phase;
 * the remaining values are reserved for future phases.
 */
enum class PreferredChannel {
    PUSH,
    EMAIL,
    SMS,
    WHATSAPP
}
