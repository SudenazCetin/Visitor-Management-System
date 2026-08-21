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
import jakarta.websocket.server.PathParam;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
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
        if (token == null || token.isBlank()) {
            closeSession(session, "Missing JWT token");
            return;
        }

        try {
            JsonWebToken jwt = jwtParser.parse(token);
            String username = jwt.getName();
            if (username == null || username.isBlank()) {
                username = jwt.getClaim("username");
            }

            if (username != null && !username.isBlank()) {
                socketService.registerSession(username, session);
            } else {
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
