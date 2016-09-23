package zstackui

import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

class EventListenerService {

    static scope = "singleton"

    RabbitmqReceiverService rabbitmqReceiverService

    @EventListener
    private void handleSessionConnected(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage())
        String username = headers.getUser().getName()
        println username + " connected with session: " + headers.getSessionId()
        rabbitmqReceiverService.addSession(username)
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage())
        String username = headers.getUser().getName()
        println username + " disconnect with session: " + headers.getSessionId()
        rabbitmqReceiverService.removeSession(username)
    }

}
