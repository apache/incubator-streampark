/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.websocket;

import org.apache.streampark.console.core.entity.Message;

import io.undertow.util.CopyOnWriteMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Map;

@Getter
@Slf4j
@Component
@ServerEndpoint(value = "/websocket/{id}")
public class WebSocketEndpoint {

    private static final Map<String, Session> SOCKET_SESSIONS = new CopyOnWriteMap<>();

    private String id;

    private Session session;

    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id) {
        log.debug("Websocket onOpen....");
        this.id = id;
        this.session = session;
        SOCKET_SESSIONS.put(id, session);
    }

    @OnClose
    public void onClose() {
        if (SOCKET_SESSIONS.containsKey(this.id)) {
            try (Session remove = SOCKET_SESSIONS.remove(this.id)) {
                if (remove != null) {
                    log.debug("Websocket onClose id: {}", this.id);
                }
            } catch (IOException e) {
                log.error("WebSocket onClose error: {}", e.getMessage(), e);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable e) {
        log.error(e.getMessage(), e);
    }

    public static void writeMessage(String socketId, String message) {
        try {
            Session session = SOCKET_SESSIONS.get(socketId);
            if (session != null) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void pushNotice(Message message) {
        try {
            Session session = SOCKET_SESSIONS.get(message.getUserId().toString());
            if (session != null) {
                session.getBasicRemote().sendObject(message);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
