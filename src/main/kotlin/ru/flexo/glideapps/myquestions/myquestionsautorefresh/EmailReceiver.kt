package ru.flexo.glideapps.myquestions.myquestionsautorefresh

import com.sun.mail.imap.IMAPFolder
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMultipart

class EmailAuthenticator(private val login: String, private val password: String) : Authenticator() {
    public override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(login, password)
    }
}

@Component
class EmailReceiver {
    val IMAP_AUTH_EMAIL = ""
    val IMAP_AUTH_PWD = ""
    val IMAP_Server = "imap.yandex.ru"
    val IMAP_Port = "993"

    val newMessageListeners = ArrayList<NewMessageListener>()
    var running = true

    fun stop() {
        running = false
    }

    fun start() {
        running = true
        run()
    }

    private fun run() {
        val properties = Properties()
        properties["mail.debug"] = "true"
        properties["mail.store.protocol"] = "imaps"
        properties["mail.imap.ssl.enable"] = "true"
        properties["mail.imap.port"] = IMAP_Port
        val auth: Authenticator = EmailAuthenticator(
            IMAP_AUTH_EMAIL,
            IMAP_AUTH_PWD
        )
        val session: Session = Session.getDefaultInstance(properties, auth)
        session.debug = false
        val store: Store = session.store
        store.connect(IMAP_Server, IMAP_AUTH_EMAIL, IMAP_AUTH_PWD)
        println("Mail Receiver Started")
        while (running) {
            checkNewMails(store)
            Thread.sleep(5000)
        }
        println("Mail Receiver Stopped")
    }

    private fun checkNewMails(store: Store) {
        try {
            val inbox = store.getFolder("INBOX") as IMAPFolder
            inbox.open(Folder.READ_WRITE)
            val glideNew = inbox.messages.filter {
                it.from.contains(InternetAddress("no-reply@glideapps.com")) &&
                        !it.flags.contains(Flags.Flag.SEEN)
            }
            println("Total new glide mails count : " + glideNew.size)
            if (glideNew.isEmpty()) return
            notifyNewMessage(glideNew.map { GlideSignInMassage(it.receivedDate, readContent(it)) })
            inbox.close()
        } catch (e: NoSuchProviderException) {
            System.err.println(e.message)
        } catch (e: MessagingException) {
            System.err.println(e.message)
        } catch (e: IOException) {
            System.err.println(e.message)
        }
    }

    fun addNewMessageListener(listener: NewMessageListener) {
        newMessageListeners.add(listener)
    }

    private fun notifyNewMessage(messages: List<GlideSignInMassage>) {
        newMessageListeners.forEach { it.onNewMessage(messages) }
    }

    private fun readContent(message: Message): String {
        return when {
            message.isMimeType("text/plain") -> {
                message.content.toString()
            }
            message.isMimeType("multipart/*") -> {
                val mimeMultipart = message.content as MimeMultipart
                getTextFromMimeMultipart(mimeMultipart)
            }
            else -> {
                "Error"
            }
        }
    }

    private fun getTextFromMimeMultipart(mimeMultipart: MimeMultipart): String {
        var result = ""
        val count = mimeMultipart.count
        for (i in 0 until count) {
            val bodyPart = mimeMultipart.getBodyPart(i)
            if (bodyPart.isMimeType("text/plain")) {
                result = """$result
                    ${bodyPart.content}""".trimIndent()
                break // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                val html = bodyPart.content as String
                result = """$result
                    ${Jsoup.parse(html).text()}""".trimIndent()
            } else if (bodyPart.content is MimeMultipart) {
                result += getTextFromMimeMultipart(bodyPart.content as MimeMultipart)
            }
        }
        return result
    }
}