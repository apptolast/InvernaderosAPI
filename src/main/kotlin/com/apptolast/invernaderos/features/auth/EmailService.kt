package com.apptolast.invernaderos.features.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
        private val mailSender: JavaMailSender,
        @Value("\${spring.mail.username}") private val fromEmail: String
) {

    fun sendPasswordResetEmail(to: String, token: String) {
        val message = SimpleMailMessage()
        message.from = fromEmail
        message.setTo(to)
        message.subject = "Restablecimiento de Contraseña - Invernaderos App"
        message.text =
                """
            Hola,
            
            Has solicitado restablecer tu contraseña. Utiliza el siguiente token para crear una nueva contraseña:
            
            $token
            
            Este token expirará en 15 minutos.
            
            Si no has solicitado esto, ignora este correo.
        """.trimIndent()

        mailSender.send(message)
    }
}
