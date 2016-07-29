package zstackui

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

class LoginController {

    def index() { }

    @MessageMapping("/login")
    @SendTo("/topic/login")
    protected String login(String msg) {
        println msg
        return msg
    }
}
