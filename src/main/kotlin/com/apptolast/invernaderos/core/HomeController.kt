package com.apptolast.invernaderos.core

import com.apptolast.invernaderos.features.sensor.CreateSensorRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
class HomeController {

    @RequestMapping("/hello")
    fun index(): CreateSensorRequest {
        return CreateSensorRequest(
            greenhouseId = UUID.randomUUID(),
            deviceId = "SENSOR-001",
            sensorType = "TEMPERATURE_INDOOR",
            minThreshold = 15.0,
            maxThreshold = 30.0
        )
    }
}