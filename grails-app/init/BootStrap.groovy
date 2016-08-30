class BootStrap {

    def asyncRabbitmqService

    def init = { servletContext ->
       asyncRabbitmqService.initialize()
    }
    def destroy = {
    }
}
