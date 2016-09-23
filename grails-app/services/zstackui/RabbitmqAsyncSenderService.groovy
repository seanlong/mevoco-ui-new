package zstackui

import com.rabbitmq.client.*
import grails.core.GrailsApplication
import groovy.json.*

import java.security.Principal
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class RabbitmqAsyncSenderService implements Runnable {

    static scope = "singleton"

    GrailsApplication grailsApplication
    RabbitmqReceiverService rabbitmqReceiverService

    String P2P_EXCHANGE, EXCHANGE_NAME, ROUTING_KEY, QUEUE_NAME, REPLY_QUEUE_NAME
    def UuidService
    private Channel channel
    final private sendingQueue = new LinkedBlockingQueue<RequestMessage>()
    final private parser = new JsonSlurper()

    def initialize() {
        P2P_EXCHANGE = "P2P"
        EXCHANGE_NAME = "BROADCAST"
        ROUTING_KEY = "zstack.message.api.portal"
        QUEUE_NAME = "zstack.newui.api.event." + UuidService.getUuid()
        REPLY_QUEUE_NAME = "zstack.newui.message." + UuidService.getUuid()

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(grailsApplication.config.getProperty('rabbitmq.host'));
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        this.channel = channel

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        channel.queueDeclare(QUEUE_NAME, false, false, true, null);
        //String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "key.event.API.API_EVENT");
        Consumer consumer = rabbitmqReceiverService.addChannelConsumer(channel)
        channel.basicConsume(QUEUE_NAME, true, consumer);

        new Thread(this).start()
    }

    def send(String message, String username) {
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
            rabbitmqReceiverService.addPendingMessage(message, corrId)
        }
    }

}
