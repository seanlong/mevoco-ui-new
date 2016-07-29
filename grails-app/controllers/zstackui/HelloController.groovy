package zstackui

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

class HelloController {

    def index() {
        render(view: "index")
    }

    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    protected String hello(String world) {
        println "hello from controller, ${world}!"
        return "hello from controller, ${world}!"
    }
}
