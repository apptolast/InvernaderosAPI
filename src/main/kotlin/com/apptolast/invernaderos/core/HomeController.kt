package com.apptolast.invernaderos.core

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class HomeController {

    @GetMapping("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "message" to "InvernaderosAPI - Simplified Schema",
            "version" to "2.0",
            "status" to "OK"
        )
    }
}