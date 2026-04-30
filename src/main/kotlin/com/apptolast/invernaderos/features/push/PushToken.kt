package com.apptolast.invernaderos.features.push

import jakarta.persistence.*
import java.time.Instant

/**
 * Token FCM (Firebase Cloud Messaging) registrado por un dispositivo de cliente
 * (móvil Android/iOS o navegador web) tras hacer login. El token se utiliza para
 * enviar notificaciones push de alertas a TODOS los dispositivos asociados a un
 * tenant.
 *
 * Reglas:
 *  - `token` es UNIQUE: un mismo dispositivo (token) sólo aparece una vez. Si
 *    el mismo token se registra desde otro user/tenant (poco probable, pero
 *    posible si hay relogin con otro user), el upsert sobrescribe el dueño.
 *  - El borrado en cascada por `user_id` y `tenant_id` garantiza que al borrar
 *    un usuario o tenant los tokens también desaparezcan.
 *  - `last_seen_at` se refresca en cada upsert; permite limpiar tokens
 *    huérfanos en una rutina de mantenimiento futura (no implementada aquí).
 */
@Entity
@Table(
    name = "push_tokens",
    schema = "metadata",
    indexes = [
        Index(name = "idx_push_tokens_tenant", columnList = "tenant_id"),
        Index(name = "idx_push_tokens_user", columnList = "user_id")
    ]
)
data class PushToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: Long,

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    var token: String,

    @Column(name = "platform", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    var platform: PushPlatform,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant = Instant.now()
)

enum class PushPlatform { ANDROID, IOS, WEB }
