package org.fnm;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import org.jboss.logging.Logger;

@WebSocket(path = "/service-registration/:instanceId/:replicaId")
public class WebSocketService {

    private static final Logger LOG = Logger.getLogger(WebSocketService.class);

    @OnTextMessage
    public String onMessage(String message, @PathParam("instanceId") String instanceId, @PathParam("replicaId") String replicaId) {
        LOG.info("Received message from " + instanceId + ":" + replicaId + ": " + message);
        return "Message received: " + message;
    }

}
