package zstackui

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

import java.security.Principal

class HelloController {

    RabbitmqSyncSenderService rabbitmqSyncSenderService
    RabbitmqAsyncSenderService rabbitmqAsyncSenderService

    def index() {
        render(view: "index")
    }

    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    protected String hello(String world) {
        return "hello from controller, ${world}!"
    }

    @MessageMapping("/sync")
    protected String syncCall(String msg, Principal principal) {
        println msg
        rabbitmqSyncSenderService.send(msg, principal.getName())
    }

    @MessageMapping("/async")
    protected  String asyncCall(String msg, Principal principal) {
        println msg
        rabbitmqAsyncSenderService.send(msg, principal.getName())
    }
}
