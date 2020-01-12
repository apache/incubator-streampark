package com.streamxhub.common.util

import com.solarmosaic.client.mail.{Envelope, EnvelopeWrappers, Mailer}
import com.solarmosaic.client.mail.configuration.SmtpConfiguration
import com.solarmosaic.client.mail.content.ContentType.MultipartTypes
import com.solarmosaic.client.mail.content.{Html, Multipart, Text}
import com.streamxhub.common.util.HttpUtils
import javax.mail.internet.InternetAddress
import javax.mail.{Authenticator, PasswordAuthentication}

package object NoticeUtils {

  case class Ding(api: String, to: String, message: String)

  case class EMail(to: String,
                   subject: String,
                   message: String,
                   user: String,
                   password: String,
                   addr: String = "",
                   port: Int = 587,
                   tls: Boolean = false,
                   subtype: String = "text"
                  ) extends Authenticator {
    override def getPasswordAuthentication: PasswordAuthentication = {
      new PasswordAuthentication(user, password)
    }
  }

  object send extends EnvelopeWrappers {

    def a(ding: Ding): Unit = {

      val body =
        s"""
           |{
           |  "msgtype": "text",
           |  "text": {
           |    "content": "${ding.message}"
           |  },
           |  "at": {
           |    "atMobiles": [
           |      ${ding.to}
           |    ],
           |    "isAtAll": false
           |  }
           |}
        """.stripMargin

      val headers = Map("content-type" -> "application/json")
      val (code, res) = HttpUtils.httpPost(ding.api, body, headers)
      println(s"result code : $code , body : $res")
    }


    def a(email: EMail): Unit = {

      val config = SmtpConfiguration(host = email.addr,
        port = email.port,
        tls = email.tls,
        debug = false,
        authenticator = Some(email)
      )
      val mailer = Mailer(config)
      val content = Multipart(
        parts = Seq(Text(email.subtype), Html(s"<p>${email.message}</p>")),
        subType = MultipartTypes.alternative
      )

      val envelope = Envelope(
        from = new InternetAddress(email.user),
        to = email.to.split(",").toSeq.map(x => new InternetAddress(x)),
        subject = email.subject,
        content = content
      )
      mailer.send(envelope)
    }
  }

}
