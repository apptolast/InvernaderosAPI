package com.apptolast.invernaderos.features.notification.domain.port.output

/**
 * Driven port for resolving basic user information needed during notification dispatch
 * and during REST controller authentication-context resolution.
 *
 * Returns null when the user does not exist; callers should treat missing users as a
 * signal to skip dispatching to the associated token and log the skip reason.
 *
 * Includes [updateLocale] so REST controllers can persist a locale change as part of
 * a preferences update without injecting the user JPA repository directly (preserves
 * the controllersShouldNotAccessRepositoriesDirectly ArchUnit rule).
 */
interface UserLookupPort {
    fun findById(userId: Long): NotificationUserSnapshot?

    /** Resolves the authenticated principal (`Authentication.name`, typically the email or username). */
    fun findByPrincipalName(principalName: String): NotificationUserSnapshot?

    /** Persists a new locale for the user. Returns false if the user is not found. */
    fun updateLocale(userId: Long, locale: String): Boolean
}

/**
 * Read-only projection of a user, used exclusively within the notification domain.
 *
 * [locale] is a BCP-47 language tag (e.g. "es-ES", "en-US") used to select the
 * i18n bundle when rendering notification content.
 */
data class NotificationUserSnapshot(
    val id: Long,
    val username: String?,
    val displayName: String?,
    val locale: String,
    val tenantId: Long
)
