package com.apptolast.invernaderos.mqtt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

/**
 * Architectural guard: prevents future regressions where someone might subscribe
 * the API to GREENHOUSE/RESPONSE. The API publishes alert echoes to RESPONSE; if
 * it also subscribed to it, an MQTT loop becomes possible (Layer 1 of the
 * defense-in-depth strategy described in AlertStateChangedMqttEchoListener).
 *
 * The test parses application.yaml and verifies that the only key holding
 * `GREENHOUSE/RESPONSE` is `spring.mqtt.topics.response` (the publish topic).
 * Any other topic key resolving to that string would mean an inbound subscription
 * to RESPONSE — forbidden.
 *
 * It also verifies the source code of MqttConfig: the topics array passed to the
 * inbound adapter must NOT reference the `response` topic property.
 */
class MqttSubscriptionGuardTest {

    @Test
    fun `application yaml only references GREENHOUSE_RESPONSE in topics_response key`() {
        val yamlStream = javaClass.classLoader.getResourceAsStream("application.yaml")
            ?: error("application.yaml not found on classpath")

        @Suppress("UNCHECKED_CAST")
        val root = Yaml().load<Map<String, Any?>>(yamlStream)

        val spring = root["spring"] as Map<String, Any?>
        val mqtt = spring["mqtt"] as Map<String, Any?>
        val topics = mqtt["topics"] as Map<String, Any?>

        topics.forEach { (key, value) ->
            val str = value?.toString().orEmpty()
            if (key != "response") {
                assertThat(str)
                    .withFailMessage(
                        "Topic key 'spring.mqtt.topics.%s' resolves to '%s' which contains 'GREENHOUSE/RESPONSE'. " +
                            "Only the 'response' key (used by MqttCommandPublisherAdapter to PUBLISH) is allowed " +
                            "to reference RESPONSE. Subscribing to RESPONSE would create an MQTT loop with the " +
                            "alert echo listener (see AlertStateChangedMqttEchoListener).",
                        key, str
                    )
                    .doesNotContain("GREENHOUSE/RESPONSE")
            }
        }
    }

    @Test
    fun `MqttConfig source does not subscribe to RESPONSE topic`() {
        val source = locateMqttConfigSource()
            ?: error("MqttConfig.kt not found — adjust the search path in this test if the file moved")

        // Find the topics array inside mqttInbound()
        val mqttInboundFnIndex = source.indexOf("fun mqttInbound")
        assertThat(mqttInboundFnIndex).isPositive()

        val arrayStart = source.indexOf("arrayOf(", mqttInboundFnIndex)
        val arrayEnd = source.indexOf(")", arrayStart)
        val topicsArray = source.substring(arrayStart, arrayEnd)

        assertThat(topicsArray)
            .withFailMessage(
                "MqttConfig.mqttInbound() topics array contains a reference to RESPONSE. " +
                    "The API must NEVER subscribe to GREENHOUSE/RESPONSE — it is the publish topic for " +
                    "alert/command echoes. See AlertStateChangedMqttEchoListener for the loop-prevention rationale."
            )
            .doesNotContain("response")
            .doesNotContain("RESPONSE")
    }

    private fun locateMqttConfigSource(): String? {
        // Resolve from the test working directory up to the repo root.
        val candidates = listOf(
            "src/main/kotlin/com/apptolast/invernaderos/config/MqttConfig.kt",
            "../src/main/kotlin/com/apptolast/invernaderos/config/MqttConfig.kt"
        )
        return candidates.firstNotNullOfOrNull { path ->
            java.io.File(path).takeIf { it.exists() }?.readText()
        }
    }
}
