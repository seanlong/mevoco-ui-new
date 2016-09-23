import zstackui.EventListenerService
import zstackui.RabbitmqAsyncSenderService
import zstackui.RabbitmqSyncSenderService

class BootStrap {

    RabbitmqSyncSenderService rabbitmqSyncSenderService
    RabbitmqAsyncSenderService rabbitmqAsyncSenderService
    EventListenerService eventListenerService

    def init = { servletContext ->
        rabbitmqSyncSenderService.initialize()
        rabbitmqAsyncSenderService.initialize()
    }

    def destroy = {
    }
}
