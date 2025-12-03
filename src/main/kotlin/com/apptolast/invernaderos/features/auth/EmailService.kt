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
                "\${app.frontend.reset-password-url:http://localhost:8080/#com.apptolast.greenhousefronts.presentation.navigation.ResetPassword?token=}"
        )
        private val resetPasswordUrl: String
) {

    fun sendPasswordResetEmail(to: String, token: String) {
        val link = "$resetPasswordUrl$token"

        val message = SimpleMailMessage()
        message.from = fromEmail
        message.setTo(to)
        message.subject = "Restablecimiento de Contraseña - Invernaderos App"
        message.text =
                """
            Hola,
            
            Has solicitado restablecer tu contraseña. Haz clic en el siguiente enlace para crear una nueva contraseña:
            
            $link
            
            Este enlace expirará en 15 minutos.
            
            Si no has solicitado esto, ignora este correo.
        """.trimIndent()

        mailSender.send(message)
    }
}
