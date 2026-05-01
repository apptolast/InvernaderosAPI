package com.apptolast.invernaderos.mqtt.service

import com.apptolast.invernaderos.features.device.DeviceRepository
import com.apptolast.invernaderos.features.setting.SettingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache that resolves a hardware/setting code (e.g. `DEV-00012`,
 * `SET-00036`) to its owning `tenantId`. Used by `DeviceStatusProcessor`
 * to attach a tenant id to each `device_current_values` flush so the
 * downstream broadcaster knows which tenant to target.
 *
 * **Bounded by the device+setting catalog size** — a few thousand entries
 * at most. Hot path is O(1) hash lookup; cold path issues one metadata
 * query per never-seen code.
 *
 * **Fail-safe**: if the metadata lookup throws (DB hiccup, missing row,
 * etc.), `resolve` returns `null` and the calling flush continues without
 * that code. The Timescale write **never aborts** because of a cache miss.
 *
 * **Thread-safe**: `ConcurrentHashMap` and idempotent puts (the same code
 * always resolves to the same tenant id; concurrent puts are harmless).
 *
 * **Invalidation**: not implemented intentionally. A code's owning tenant
 * is stable for its lifetime — devices and settings get reassigned by
 * deleting + recreating with a new code, never by editing the old one.
 * If that invariant ever changes, add explicit invalidation on the CRUD
 * use cases.
 */
@Component
class CodeToTenantCache(
    private val deviceRepository: DeviceRepository,
    private val settingRepository: SettingRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val cache = ConcurrentHashMap<String, Long>()

    /**
     * Returns the `tenantId` that owns the given hardware/setting code, or
     * `null` if it cannot be resolved (unknown code, DB error, etc.).
     */
    fun resolve(code: String): Long? {
        cache[code]?.let { return it }

        val resolved = try {
            lookup(code)
        } catch (e: Exception) {
            logger.warn("Failed to resolve tenantId for code={}: {}", code, e.message)
            null
        }
        if (resolved != null) cache[code] = resolved
        return resolved
    }

    @Transactional("metadataTransactionManager", readOnly = true)
    private fun lookup(code: String): Long? {
        return when {
            code.startsWith("DEV-") -> deviceRepository.findByCode(code)?.tenantId
            code.startsWith("SET-") -> settingRepository.findByCode(code)?.tenantId
            else -> null
        }
    }
}
