package com.example.taskapi.auth

interface AuthEmailSender {
    fun sendVerification(email: String, token: String, endpoint: String)
    fun sendPasswordReset(email: String, token: String, endpoint: String)
}

class ConsoleAuthEmailSender : AuthEmailSender {
    override fun sendVerification(email: String, token: String, endpoint: String) {
        System.getLogger("AuthEmail").log(System.Logger.Level.INFO, "Development verification for {0}: POST {1} with token {2}", email, endpoint, token)
    }

    override fun sendPasswordReset(email: String, token: String, endpoint: String) {
        System.getLogger("AuthEmail").log(System.Logger.Level.INFO, "Development password reset for {0}: POST {1} with token {2}", email, endpoint, token)
    }
}

class SmtpAuthEmailSender(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val from: String,
    private val startTls: Boolean
) : AuthEmailSender {
    override fun sendVerification(email: String, token: String, endpoint: String) =
        send(email, "Verify your Task API account", endpoint, token)

    override fun sendPasswordReset(email: String, token: String, endpoint: String) =
        send(email, "Reset your Task API password", endpoint, token)

    private fun send(email: String, subject: String, endpoint: String, token: String) {
        val properties = java.util.Properties().apply {
            setProperty("mail.smtp.host", host)
            setProperty("mail.smtp.port", port.toString())
            setProperty("mail.smtp.auth", username.isNotBlank().toString())
            setProperty("mail.smtp.starttls.enable", startTls.toString())
        }
        val authenticator = if (username.isBlank()) null else object : jakarta.mail.Authenticator() {
            override fun getPasswordAuthentication() = jakarta.mail.PasswordAuthentication(username, password)
        }
        val session = jakarta.mail.Session.getInstance(properties, authenticator)
        val message = jakarta.mail.internet.MimeMessage(session).apply {
            setFrom(this@SmtpAuthEmailSender.from)
            setRecipients(jakarta.mail.Message.RecipientType.TO, email)
            setSubject(subject)
            setText("Use this one-time token with POST $endpoint:\n\n{\"token\":\"$token\"}")
        }
        jakarta.mail.Transport.send(message)
    }
}