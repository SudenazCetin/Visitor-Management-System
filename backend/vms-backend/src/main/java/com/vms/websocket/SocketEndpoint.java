package com.vms.websocket;

import com.vms.service.SocketService;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@ServerEndpoint("/ws/notifications/{token}")
@ApplicationScoped
public class SocketEndpoint {

    private static final Logger LOG = Logger.getLogger(SocketEndpoint.class);

    @Inject
    JWTParser jwtParser;

    @Inject
    SocketService socketService;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        String rawToken = token;

        if ((rawToken == null || rawToken.isBlank()) && session.getRequestParameterMap() != null) {
            List<String> tokens = session.getRequestParameterMap().get("token");
            if (tokens != null && !tokens.isEmpty()) {
                rawToken = tokens.get(0);
            }
        }

        if (rawToken == null || rawToken.isBlank()) {
            LOG.warn("WebSocket connection rejected: Missing JWT token");
            closeSession(session, "Missing JWT token");
            return;
        }

        try {
            if (rawToken.startsWith("Bearer ")) {
                rawToken = rawToken.substring(7);
            }
            if (rawToken.contains("%")) {
                rawToken = URLDecoder.decode(rawToken, StandardCharsets.UTF_8);
            }

            JsonWebToken jwt = jwtParser.parse(rawToken);
            String username = jwt.getName();
            if (username == null || username.isBlank()) {
                username = jwt.getClaim("username");
            }
            if (username == null || username.isBlank()) {
                username = jwt.getSubject();
            }

            if (username != null && !username.isBlank()) {
                socketService.registerSession(username, session);
                LOG.infof("WebSocket session opened & registered for user: %s", username);
            } else {
                LOG.warn("WebSocket connection rejected: Invalid JWT claims");
                closeSession(session, "Invalid JWT claims");
            }
        } catch (ParseException e) {
            LOG.warnf("WebSocket connection rejected: invalid JWT token (%s)", e.getMessage());
            closeSession(session, "Invalid JWT token");
        } catch (Exception e) {
            LOG.error("WebSocket connection error during auth", e);
            closeSession(session, "Internal auth error");
        }
    }

    @OnClose
    public void onClose(Session session) {
        socketService.removeSession(session);
        LOG.info("WebSocket session closed.");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.warnf("WebSocket session error: %s", throwable != null ? throwable.getMessage() : "Unknown error");
        socketService.removeSession(session);
    }

    private void closeSession(Session session, String reason) {
        try {
            if (session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
            }
        } catch (Exception ignored) {
        }
    }
}
