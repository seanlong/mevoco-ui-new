package zstackui

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.rabbitmq.client.*
import grails.core.GrailsApplication
import groovy.json.*

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

class RabbitmqSyncSenderService implements Runnable {

    static scope = "singleton"

    def UuidService

    String P2P_EXCHANGE, REPLY_QUEUE_NAME, ROUTING_KEY
    GrailsApplication grailsApplication
    RabbitmqReceiverService rabbitmqReceiverService
    private Channel channel
    final private sendingQueue = new LinkedBlockingDeque<RequestMessage>()
    final private parser = new JsonSlurper()

    def initialize() {
        P2P_EXCHANGE = "P2P"
        ROUTING_KEY = "zstack.message.api.portal"
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
        Consumer consumer = rabbitmqReceiverService.addChannelConsumer(channel)
        channel.basicConsume(REPLY_QUEUE_NAME, true, consumer)

        new Thread(this).start()
    }

    def String send(String message, String username) {
        sendingQueue.add(new RequestMessage(username, message))
    }

    def void run() {
        while (true) {
            def message = sendingQueue.take()
            def corrId = UUID.randomUUID().toString().replace("-", "")
            def obj = parser.parseText(message.getMessage())
            def body = obj.values()[0]
            body.put("serviceId", "api.portal")
            body.put("id", corrId)
            def bodyHeaders = ["replyTo": REPLY_QUEUE_NAME, "noReply": "false", "correlationId": corrId]
            body.put("headers", bodyHeaders)
            BasicProperties props = new AMQP.BasicProperties.Builder()
                    .correlationId(corrId)
                    .replyTo(REPLY_QUEUE_NAME)
                    .build()
            channel.basicPublish(P2P_EXCHANGE, ROUTING_KEY, props, JsonOutput.toJson(obj).getBytes("UTF-8"))
            rabbitmqReceiverService.addPendingMessage(message.getUsername(), body["session"], corrId)
        }
    }
}
