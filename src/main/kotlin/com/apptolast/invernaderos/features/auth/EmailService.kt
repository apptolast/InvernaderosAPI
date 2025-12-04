package com.apptolast.invernaderos.features.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
        private val mailSender: JavaMailSender,
        @Value("\${spring.mail.username}") private val fromEmail: String,
        @Value(
                "\${app.frontend.reset-password-web-url:http://localhost:8080/#com.apptolast.greenhousefronts.presentation.navigation.ResetPassword%2F}"
        )
        private val resetPasswordWebUrl: String,
        @Value("\${app.frontend.reset-password-mobile-url:invernaderos://reset-password%2F}")
        private val resetPasswordMobileUrl: String
) {

    fun sendPasswordResetEmail(to: String, token: String) {
        val webLink = "$resetPasswordWebUrl$token"
        val mobileLink = "$resetPasswordMobileUrl$token"

        val message = SimpleMailMessage()
        message.from = fromEmail
        message.setTo(to)
        message.subject = "Restablecimiento de Contraseña - Invernaderos App"
        message.text =
                """
            Hola,

            Has solicitado restablecer tu contraseña.

            Si estás en PC o Web, usa este enlace:
            $webLink

            Si estás en la App Móvil, usa este enlace:
            $mobileLink

            Estos enlaces expirarán en 15 minutos.

            Su token para resetear la contraseña :

            token : $token

            Si no has solicitado esto, ignora este correo.
        """.trimIndent()

        mailSender.send(message)
    }
}