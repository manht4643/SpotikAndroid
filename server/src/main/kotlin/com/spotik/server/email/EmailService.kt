package com.spotik.server.email

import io.ktor.server.config.*
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import java.util.Properties

/**
 * Sends OTP verification emails via SMTP (Gmail, Yandex, etc.).
 *
 * Gmail setup:
 * 1. Enable 2FA on your Google account
 * 2. Create an App Password at https://myaccount.google.com/apppasswords
 * 3. Use the 16-char app password as SMTP_PASSWORD
 */
object EmailService {
    private val log = LoggerFactory.getLogger("EmailService")

    private lateinit var session: Session
    private lateinit var fromAddress: String
    private var enabled = false

    fun init(config: ApplicationConfig) {
        val smtp = config.configOrNull("smtp")
        if (smtp == null) {
            log.warn("SMTP config not found — email verification disabled (codes will be logged)")
            return
        }
        val host = smtp.propertyOrNull("host")?.getString() ?: "smtp.gmail.com"
        val port = smtp.propertyOrNull("port")?.getString() ?: "587"
        val user = smtp.propertyOrNull("username")?.getString().orEmpty()
        val pass = smtp.propertyOrNull("password")?.getString().orEmpty()
        fromAddress = smtp.propertyOrNull("from")?.getString() ?: user

        if (user.isBlank() || pass.isBlank()) {
            log.warn("SMTP credentials missing — email verification disabled (codes will be logged)")
            return
        }

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", host)
            put("mail.smtp.port", port)
            put("mail.smtp.ssl.trust", host)
        }

        session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(user, pass)
        })
        enabled = true
        log.info("EmailService initialised — host=$host, from=$fromAddress")
    }

    /**
     * @return true if the email was sent successfully (or logged when SMTP is not configured)
     */
    fun sendVerificationCode(toEmail: String, code: String): Boolean {
        if (!enabled) {
            // Fallback for development: just log the code
            log.info("📧 [DEV] Verification code for $toEmail: $code")
            return true
        }
        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromAddress, "Love u"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Love u — код подтверждения"
                setContent(
                    """
                    <div style="font-family:sans-serif;max-width:400px;margin:auto;padding:30px;
                                background:linear-gradient(135deg,#1a1a2e,#16213e);border-radius:16px;
                                color:#fff;text-align:center">
                        <h2 style="margin-bottom:8px">Love u 💜</h2>
                        <p style="color:#aaa;font-size:14px">Ваш код подтверждения:</p>
                        <div style="font-size:36px;font-weight:bold;letter-spacing:8px;
                                    margin:24px 0;color:#c084fc">$code</div>
                        <p style="color:#888;font-size:12px">Код действителен 5 минут.<br>
                        Если вы не запрашивали код, просто проигнорируйте это письмо.</p>
                    </div>
                    """.trimIndent(),
                    "text/html; charset=utf-8",
                )
            }
            Transport.send(message)
            log.info("Verification email sent to $toEmail")
            true
        } catch (e: Exception) {
            log.error("Failed to send email to $toEmail", e)
            false
        }
    }

    /** Helper to safely read optional config blocks */
    private fun ApplicationConfig.configOrNull(path: String): ApplicationConfig? =
        try { config(path) } catch (_: Exception) { null }
}

