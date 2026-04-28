package com.apptolast.invernaderos.features.alert.domain.port.output

/**
 * Output port for echoing alert state transitions to the external transport
 * (typically MQTT GREENHOUSE/RESPONSE so the Node-RED bridge stays in sync).
 *
 * The infrastructure adapter chooses the wire format and the transport, so the
 * domain remains free of MQTT, JSON or any framework concern.
 */
interface AlertEchoPublisherPort {
    /**
     * Echo a state transition. The implementation is responsible for serialising
     * `value` into whatever format the receiver expects (JSON int, boolean, etc.).
     */
    fun publish(code: String, value: Int)
}
