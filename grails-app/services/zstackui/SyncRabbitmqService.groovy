package zstackui

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.rabbitmq.client.*
import grails.core.GrailsApplication
import groovy.json.*

class SyncRabbitmqService {

    static scope = "singleton"

    def UuidService

    String P2P_EXCHANGE, REPLY_QUEUE_NAME, REQUEST_QUEUE_NAME
    GrailsApplication grailsApplication
    QueueingConsumer consumer
    Channel channel
    JsonSlurper parser

    def initialize() {
        P2P_EXCHANGE = "P2P"
        REQUEST_QUEUE_NAME = "zstack.message.api.portal"
        REPLY_QUEUE_NAME = "zstack.newui.message." + UuidService.getUuid()

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(grailsApplication.config.getProperty("rabbitmq.host"))
        Connection connection = factory.newConnection()
        Channel channel = connection.createChannel()
        this.channel = channel

        def args = ["alternate-exchange": "NO_ROUTE"]
        channel.exchangeDeclare(P2P_EXCHANGE, "topic", true, false, args as Map<String, Object>)
        channel.queueDeclare(REPLY_QUEUE_NAME, false, false, true, null)
        channel.queueBind(REPLY_QUEUE_NAME, P2P_EXCHANGE, REPLY_QUEUE_NAME)
        this.consumer = new QueueingConsumer(channel)
        channel.basicConsume(REPLY_QUEUE_NAME, true, this.consumer)

        parser = new JsonSlurper()
    }

    def String send(String message) {
        def corrId = UUID.randomUUID().toString().replace("-", "")
        def obj = parser.parseText(message)
        def body = obj.values()[0]
        body.put("serviceId", "api.portal")
        body.put("id", corrId)
        def bodyHeaders = ["replyTo": REPLY_QUEUE_NAME, "noReply": "false", "correlationId": corrId]
        body.put("headers", bodyHeaders)
        message = JsonOutput.toJson(obj)

        BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(corrId)
                .replyTo(REPLY_QUEUE_NAME)
                .build()
        channel.basicPublish(P2P_EXCHANGE, REQUEST_QUEUE_NAME, props, message.getBytes("UTF-8"))

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery()
            System.out.println(new String(delivery.getBody()))
            obj = parser.parse(delivery.getBody(), "UTF-8")
            body = obj.values()[0]
            bodyHeaders = body["headers"]
            if (bodyHeaders["isReply"] == "true" && bodyHeaders["correlationId"] == corrId) {
                println(JsonOutput.toJson(body))
                return JsonOutput.toJson(body)
            }
        }
    }
}
