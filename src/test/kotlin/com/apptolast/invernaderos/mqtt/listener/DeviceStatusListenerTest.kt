package com.apptolast.invernaderos.mqtt.listener

import com.apptolast.invernaderos.mqtt.service.DeviceStatusProcessor
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.integration.support.MessageBuilder

class DeviceStatusListenerTest {

    private val processor = mockk<DeviceStatusProcessor>()
    private val listener = DeviceStatusListener(processor, ObjectMapper())

    @Test
    fun `should extract device reference from greenhouse alert topic`() {
        justRun { processor.processStatusUpdate(any(), any(), any()) }

        listener.handleDeviceStatus(
            MessageBuilder.withPayload("""{"id":"ALT-00010","value":false}""")
                .setHeader(MqttHeaders.RECEIVED_TOPIC, "greenhouse/GW-001/alerts/ALT-00010")
                .build()
        )

        verify(exactly = 1) { processor.processStatusUpdate("ALT-00010", "0", "GW-001") }
    }

    @Test
    fun `should keep device reference null for legacy status topic`() {
        justRun { processor.processStatusUpdate(any(), any(), any()) }

        listener.handleDeviceStatus(
            MessageBuilder.withPayload("""{"id":"ALT-00010","value":true}""")
                .setHeader(MqttHeaders.RECEIVED_TOPIC, "GREENHOUSE/STATUS")
                .build()
        )

        verify(exactly = 1) { processor.processStatusUpdate("ALT-00010", "1", null) }
    }
}
