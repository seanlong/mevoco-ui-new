package zstackui

import com.rabbitmq.client.*
import grails.core.GrailsApplication
import groovy.json.*

class SyncRabbitmqService {

    static scope = "singleton"

    def UuidService

    GrailsApplication grailsApplication
    QueueingConsumer consumer
    String P2P_EXCHANGE, REPLY_QUEUE_NAME

    def initialize() {
        this.P2P_EXCHANGE = "P2P"
        this.REPLY_QUEUE_NAME = "zstack.newui.message." + UuidService.getUuid()

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(grailsApplication.config.getProperty("rabbitmq.host"))
        Connection connection = factory.newConnection()
        Channel channel = connection.createChannel()

        def args = ["alternate-exchange": "NO_ROUTE"]
        channel.exchangeDeclare(this.P2P_EXCHANGE, "topic", true, false, args as Map<String, Object>)
        channel.queueDeclare(this.REPLY_QUEUE_NAME, false, false, true, null)
        channel.queueBind(this.REPLY_QUEUE_NAME, this.P2P_EXCHANGE, this.REPLY_QUEUE_NAME)
        this.consumer = new QueueingConsumer(channel)
        channel.basicConsume(this.REPLY_QUEUE_NAME, true, this.consumer)
    }

    def send(String message) {
        def corrId = UUID.randomUUID().toString().replace("-", "")
        def jsonSlurper = new JsonSlurper()
        def obj = jsonSlurper.parseText(message)
        def body = obj.values()[0]
        body.put("serviceId", "api.portal")
        body.put("id", corrId)
        def bodyHeaders = ["replyTo": this.REPLY_QUEUE_NAME, "noReply": "false", "correlationId": corrId]
        body.put("headers", bodyHeaders)
        message = JsonOutput.toJson(obj)
        println(message)
    }
}
