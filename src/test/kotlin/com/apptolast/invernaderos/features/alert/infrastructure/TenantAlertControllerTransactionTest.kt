package com.apptolast.invernaderos.features.alert.infrastructure

import com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.TenantAlertController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

class TenantAlertControllerTransactionTest {

    @Test
    fun `tenant alert write endpoints use metadata transaction manager`() {
        listOf(
            TenantAlertController::class.java.getMethod(
                "create",
                Long::class.javaPrimitiveType,
                Class.forName("com.apptolast.invernaderos.features.alert.dto.request.AlertCreateRequest"),
            ),
            TenantAlertController::class.java.getMethod(
                "update",
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Class.forName("com.apptolast.invernaderos.features.alert.dto.request.AlertUpdateRequest"),
            ),
            TenantAlertController::class.java.getMethod(
                "delete",
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
            ),
        ).forEach { method ->
            val transactional = method.getAnnotation(Transactional::class.java)
            assertThat(transactional)
                .describedAs("${method.name} must run in metadataTransactionManager")
                .isNotNull
            assertThat(transactional.value).isEqualTo("metadataTransactionManager")
        }
    }
}
