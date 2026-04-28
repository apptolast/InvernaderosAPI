package com.apptolast.invernaderos.mqtt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Architectural guard: prevents future regressions where someone might subscribe
 * the API to GREENHOUSE/RESPONSE. The API publishes alert echoes to RESPONSE; if
 * it also subscribed to it, an MQTT loop becomes possible (Layer 1 of the
 * defense-in-depth strategy described in AlertStateChangedMqttEchoListener).
 *
 * Verifies four invariants:
 *  1. application.yaml only references GREENHOUSE/RESPONSE under
 *     `spring.mqtt.topics.response` (the publish-only key).
 *  2. The `mqttInbound()` topics array literal in MqttConfig.kt does not
 *     reference the response property name.
 *  3. The wildcard pattern `greenhouseMultiTenantPattern` (`GREENHOUSE/+`) is
 *     NOT included in the inbound topics array — including it would match
 *     `GREENHOUSE/RESPONSE` and defeat L1.
 *  4. The MqttConfig source file is actually located (no silent pass when the
 *     path resolution fails).
 */
class MqttSubscriptionGuardTest {

    @Test
    fun `application yaml only references GREENHOUSE_RESPONSE in topics_response key`() {
        val yamlStream = javaClass.classLoader.getResourceAsStream("application.yaml")
            ?: error("application.yaml not found on classpath — guard cannot run")

        @Suppress("UNCHECKED_CAST")
        val root = Yaml().load<Map<String, Any?>>(yamlStream)

        val spring = root["spring"] as? Map<String, Any?>
            ?: error("spring section missing in application.yaml")
        val mqtt = spring["mqtt"] as? Map<String, Any?>
            ?: error("spring.mqtt section missing in application.yaml")
        val topics = mqtt["topics"] as? Map<String, Any?>
            ?: error("spring.mqtt.topics section missing in application.yaml")

        topics.forEach { (key, value) ->
            val str = value?.toString().orEmpty()
            if (key != "response") {
                assertThat(str)
                    .withFailMessage(
                        "Topic key 'spring.mqtt.topics.%s' resolves to '%s' which contains 'GREENHOUSE/RESPONSE'. " +
                            "Only the 'response' key (used by MqttCommandPublisherAdapter and " +
                            "AlertMqttEchoPublisherAdapter to PUBLISH) is allowed to reference RESPONSE. " +
                            "Subscribing to RESPONSE would create an MQTT loop with the alert echo listener " +
                            "(see AlertStateChangedMqttEchoListener).",
                        key, str
                    )
                    .doesNotContain("GREENHOUSE/RESPONSE")
            }
        }
    }

    @Test
    fun `MqttConfig source does not subscribe to RESPONSE topic`() {
        val source = readMqttConfigSource()

        val mqttInboundFnIndex = source.indexOf("fun mqttInbound")
        assertThat(mqttInboundFnIndex)
            .withFailMessage("mqttInbound() function not found in MqttConfig.kt — guard test must be updated")
            .isPositive()

        // Find the topics array literal — handle both single-line and multi-line arrayOf(...) forms.
        val arrayOfStart = source.indexOf("arrayOf(", mqttInboundFnIndex)
        assertThat(arrayOfStart)
            .withFailMessage("arrayOf(...) call not found inside mqttInbound() — guard test must be updated")
            .isPositive()

        val arrayOfEnd = matchingClose(source, openIndex = arrayOfStart + "arrayOf".length)
        assertThat(arrayOfEnd).isPositive()
        val topicsArray = source.substring(arrayOfStart, arrayOfEnd + 1)

        // Forbid both literal RESPONSE strings AND the property name `response` (which would
        // resolve to it via @Value injection).
        assertThat(topicsArray)
            .withFailMessage(
                "MqttConfig.mqttInbound() topics array contains a reference to RESPONSE. " +
                    "The API must NEVER subscribe to GREENHOUSE/RESPONSE — it is the publish topic " +
                    "for alert/command echoes. See AlertStateChangedMqttEchoListener."
            )
            .doesNotContain("Response")
            .doesNotContain("RESPONSE")
            .doesNotContain("response")

        // Forbid the multi-tenant wildcard property too — including it would match RESPONSE.
        assertThat(topicsArray)
            .withFailMessage(
                "MqttConfig.mqttInbound() topics array contains 'greenhouseMultiTenantPattern' which is " +
                    "configured as 'GREENHOUSE/+' and would match GREENHOUSE/RESPONSE. " +
                    "Including this in the inbound subscription defeats L1 of the loop-prevention defense."
            )
            .doesNotContain("greenhouseMultiTenant")
    }

    /**
     * Reads MqttConfig.kt as text. Resolves relative to the project root (located by
     * walking upwards from `user.dir` until `build.gradle.kts` is found). Fails the
     * test loudly if the file cannot be located, never silently passes.
     */
    private fun readMqttConfigSource(): String {
        val relative = "src/main/kotlin/com/apptolast/invernaderos/config/MqttConfig.kt"
        val projectRoot = locateProjectRoot()
            ?: error("Could not locate project root (no build.gradle.kts found above ${Paths.get("").toAbsolutePath()})")
        val file = projectRoot.resolve(relative)
        check(Files.exists(file)) {
            "MqttConfig.kt not found at $file — guard test must be updated if file moved"
        }
        return Files.readString(file)
    }

    private fun locateProjectRoot(): Path? {
        var dir: Path? = Paths.get("").toAbsolutePath()
        while (dir != null) {
            if (Files.exists(dir.resolve("build.gradle.kts")) || Files.exists(dir.resolve("build.gradle"))) {
                return dir
            }
            dir = dir.parent
        }
        return null
    }

    private fun matchingClose(text: String, openIndex: Int): Int {
        // openIndex points at '(' position
        var depth = 0
        for (i in openIndex until text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
