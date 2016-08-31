package zstackui

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

class SyncMessageController {

    def syncRabbitmqService

    def index() { }

    @MessageMapping("/sync")
    @SendTo("/topic/sync")
    protected String call(String msg) {
        println msg
        def reply = syncRabbitmqService.send(msg)
        return reply
    }

}
