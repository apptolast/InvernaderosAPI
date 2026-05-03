package com.apptolast.invernaderos.features.notification.domain.port.output

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.notification.domain.model.AlertAgingDetectedEvent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationContent
import com.apptolast.invernaderos.features.notification.domain.model.NotificationRecipient
import com.apptolast.invernaderos.features.notification.domain.model.NotificationType

/**
 * Driven port for rendering the final i18n notification payload for a specific recipient.
 *
 * The adapter implementation uses Spring MessageSource with ResourceBundle to select the
 * correct locale bundle for [recipient.locale] and resolve severity translations.
 *
 * [agingContext] is non-null only when [type] is [NotificationType.ALERT_AGING]; it carries
 * the age information needed to build the aging-specific title and body.
 */
interface NotificationContentRendererPort {
    fun render(
        type: NotificationType,
        alert: Alert,
        change: AlertStateChange?,
        recipient: NotificationRecipient,
        severity: NotificationSeveritySnapshot,
        agingContext: AlertAgingDetectedEvent?
    ): NotificationContent
}
