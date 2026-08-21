package com.vms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vms.websocket.SocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SocketService {

    private static final Logger LOG = Logger.getLogger(SocketService.class);

    // Username -> Set of active WebSocket Sessions
    private final ConcurrentMap<String, Set<Session>> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SocketService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void registerSession(String username, Session session) {
        if (username == null || username.isBlank() || session == null) {
            return;
        }
        String key = username.trim().toLowerCase();
        userSessions.computeIfAbsent(key, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(session);
        session.getUserProperties().put("username", key);
        LOG.infof("WebSocket session registered for user: %s (Total sessions for user: %d)",
                key, userSessions.get(key).size());
    }

    public void removeSession(Session session) {
        if (session == null) return;
        String username = (String) session.getUserProperties().get("username");
        if (username != null) {
            String key = username.trim().toLowerCase();
            if (userSessions.containsKey(key)) {
                Set<Session> sessions = userSessions.get(key);
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(key);
                }
                LOG.infof("WebSocket session closed for user: %s", key);
            }
        }
    }

    public void sendToUser(String username, SocketMessage message) {
        if (username == null || username.isBlank()) return;

        String key = username.trim().toLowerCase();
        Set<Session> sessions = userSessions.get(key);
        if (sessions == null || sessions.isEmpty()) {
            LOG.debugf("No active WebSocket session for user: %s. Message buffered in DB.", key);
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(jsonPayload, result -> {
                        if (result.getException() != null) {
                            LOG.warnf("Failed to deliver WebSocket message to user %s: %s",
                                    username, result.getException().getMessage());
                        }
                    });
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error serializing WebSocket message for user: %s", username);
        }
    }

    public void broadcast(SocketMessage message) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            userSessions.values().forEach(sessions -> {
                for (Session session : sessions) {
                    if (session.isOpen()) {
                        session.getAsyncRemote().sendText(jsonPayload);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Error broadcasting WebSocket message", e);
        }
    }
}
