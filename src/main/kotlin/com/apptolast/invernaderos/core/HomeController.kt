package com.apptolast.invernaderos.core

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HomeController {

    @GetMapping("/hello")
    fun index(): Map<String, Any> {
        return mapOf(
            "message" to "InvernaderosAPI - Simplified Schema",
            "version" to "2.0",
            "status" to "OK"
        )
    }
}