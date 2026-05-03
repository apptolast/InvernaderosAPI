package com.apptolast.invernaderos.features.notification.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

@Configuration
class NotificationI18nConfig {

    @Bean("notificationMessageSource")
    fun notificationMessageSource(): ResourceBundleMessageSource =
        ResourceBundleMessageSource().apply {
            setBasename("i18n/notifications")
            setDefaultEncoding("UTF-8")
            setUseCodeAsDefaultMessage(true)
        }
}
