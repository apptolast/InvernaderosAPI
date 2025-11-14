package com.apptolast.invernaderos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class InvernaderosApplication

fun main(args: Array<String>) {
	runApplication<InvernaderosApplication>(*args)
}
