package com.apptolast.invernaderos.features.command.domain.port.output

interface CommandPublisherPort {
    fun publish(code: String, value: String)
}
