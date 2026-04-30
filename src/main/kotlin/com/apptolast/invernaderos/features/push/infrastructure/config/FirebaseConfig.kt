package com.apptolast.invernaderos.features.push.infrastructure.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

/**
 * Inicializa el SDK de Firebase Admin para enviar notificaciones push FCM
 * desde el backend.
 *
 * Resolución del fichero de credenciales (Service Account JSON):
 *   1) Property `firebase.service-account-path` (application.yaml o env var
 *      FIREBASE_SERVICE_ACCOUNT_PATH).
 *   2) Variable de entorno estándar `GOOGLE_APPLICATION_CREDENTIALS`.
 *
 * Degradación graciosa: si NINGUNO de los dos está configurado o el fichero
 * no se puede leer, se registra un WARN y NO se exponen los beans
 * `FirebaseApp` / `FirebaseMessaging`. La aplicación arranca igualmente y los
 * consumidores (`FcmPushService`) reciben un `FirebaseMessaging?` nulo y
 * desactivan el envío. Esto es deliberado: el sistema debe seguir creando
 * alertas y resolviéndolas vía MQTT/REST aunque FCM no esté operativo.
 *
 * Nota de seguridad: el JSON contiene una clave privada. NUNCA se commitea;
 * se inyecta vía variable de entorno o se monta como secret en Kubernetes.
 *
 * Re-arranque idempotente: si la JVM ya tiene un `FirebaseApp` por defecto
 * inicializado (caso típico en tests con contexto reciclado), reutilizamos
 * la instancia existente en lugar de reinicializar.
 */
@Configuration
class FirebaseConfig(
    @Value("\${firebase.service-account-path:}")
    private val serviceAccountPathProperty: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun firebaseApp(): FirebaseApp? {
        val path = resolveCredentialsPath() ?: run {
            logger.warn(
                "Firebase service account JSON not configured " +
                        "(neither firebase.service-account-path nor GOOGLE_APPLICATION_CREDENTIALS) — " +
                        "FCM push notifications DISABLED. Alerts will continue to flow via WebSocket."
            )
            return null
        }

        val existing = FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
        if (existing != null) {
            logger.info("Reusing existing FirebaseApp instance")
            return existing
        }

        return try {
            FileInputStream(path).use { stream ->
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build()
                val app = FirebaseApp.initializeApp(options)
                logger.info("FirebaseApp initialised from {}", path)
                app
            }
        } catch (ex: Exception) {
            logger.warn(
                "Failed to initialise FirebaseApp from path={} — FCM push notifications DISABLED",
                path, ex
            )
            null
        }
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp?): FirebaseMessaging? {
        return firebaseApp?.let { FirebaseMessaging.getInstance(it) }
    }

    private fun resolveCredentialsPath(): String? {
        if (serviceAccountPathProperty.isNotBlank()) return serviceAccountPathProperty
        val envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        if (!envPath.isNullOrBlank()) return envPath
        return null
    }
}
