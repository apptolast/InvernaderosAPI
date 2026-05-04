package com.apptolast.invernaderos

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
class InvernaderosApplication

fun main(args: Array<String>) {
	runApplication<InvernaderosApplication>(*args)
}
