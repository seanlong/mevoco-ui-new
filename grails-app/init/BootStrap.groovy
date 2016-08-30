class BootStrap {

    def asyncRabbitmqService
    def syncRabbitmqService

    def init = { servletContext ->
        asyncRabbitmqService.initialize()
        syncRabbitmqService.initialize()
        def message = """
            {
              "org.zstack.header.image.APIQueryImageMsg": {
              "count": true,
              "start": 0,
              "replyWithCount": true,
              "conditions": [],
              "session": {
                  "uuid": "4fe9b1b1df6046979c6b789d2ad14330",
                  "callid": "api-cFKCG25w"
                }
              }
            }
        """
        syncRabbitmqService.send(message)
    }
    def destroy = {
    }
}
