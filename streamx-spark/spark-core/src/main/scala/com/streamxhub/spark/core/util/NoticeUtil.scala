/**
  * Copyright (c) 2019 The StreamX Project
  * <p>
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  * <p>
  * http://www.apache.org/licenses/LICENSE-2.0
  * <p>
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */

package com.streamxhub.spark.core.util

import com.solarmosaic.client.mail.EnvelopeWrappers
import com.solarmosaic.client.mail.content.ContentType.MultipartTypes
import javax.mail.internet.InternetAddress
import javax.mail.{Authenticator, PasswordAuthentication}

package object NoticeUtil {

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
    override def getPasswordAuthentication(): PasswordAuthentication = {
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
      val (code, res) = Utils.httpPost(ding.api, body, headers)
      println(s"result code : $code , body : $res")
    }


    def a(email: EMail): Unit = {

      import com.solarmosaic.client.mail._
      import com.solarmosaic.client.mail.configuration._
      import com.solarmosaic.client.mail.content._

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
