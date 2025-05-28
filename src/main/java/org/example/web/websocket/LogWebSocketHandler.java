package org.example.web.websocket;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.socket.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class LogWebSocketHandler implements WebSocketHandler {

    private static final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();

    public static void broadcast(String msg) {
        sessions.forEach(s -> {
            try { s.sendMessage(new TextMessage(msg)); } catch (Exception ignore) {}
        });
    }

    @Override public void afterConnectionEstablished(WebSocketSession s) {
        sessions.add(s);
    }
    @Override public void handleMessage(WebSocketSession s, WebSocketMessage<?> m) {}
    @Override public void handleTransportError(WebSocketSession s, Throwable e) {}
    @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus cs) {
        sessions.remove(s);
    }
    @Override public boolean supportsPartialMessages() { return false; }
}
