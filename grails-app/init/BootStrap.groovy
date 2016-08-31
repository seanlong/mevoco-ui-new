class BootStrap {

    def asyncRabbitmqService
    def syncRabbitmqService

    def init = { servletContext ->
        asyncRabbitmqService.initialize()
        syncRabbitmqService.initialize()

    }
    def destroy = {
    }
}
