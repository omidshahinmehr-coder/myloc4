package ir.example.locationsms

import java.io.File
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

object EmailSender {

    /**
     * Sends an email with one or more file attachments over SMTP.
     * Must be called from a background thread/coroutine — this performs
     * blocking network I/O. Returns true on success, false on any failure.
     */
    fun sendLogEmail(
        smtpHost: String,
        smtpPort: String,
        senderEmail: String,
        senderPassword: String,
        recipientEmail: String,
        subject: String,
        bodyText: String,
        attachmentFiles: List<File>
    ): Boolean {
        return try {
            val props = Properties().apply {
                put("mail.smtp.host", smtpHost)
                put("mail.smtp.port", smtpPort)
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.ssl.trust", smtpHost)
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                setSubject(subject)
            }

            val multipart = MimeMultipart()

            val textPart = MimeBodyPart().apply { setText(bodyText) }
            multipart.addBodyPart(textPart)

            attachmentFiles.forEach { file ->
                if (file.exists()) {
                    val attachmentPart = MimeBodyPart()
                    attachmentPart.attachFile(file)
                    multipart.addBodyPart(attachmentPart)
                }
            }

            message.setContent(multipart)
            Transport.send(message)
            true
        } catch (e: Exception) {
            false
        }
    }
}
