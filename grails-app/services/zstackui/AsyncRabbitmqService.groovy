package zstackui

import com.rabbitmq.client.*
import grails.core.GrailsApplication

class AsyncRabbitmqService {

    static scope = "singleton"

    GrailsApplication grailsApplication

    def UuidService
    def lastMessage

    def initialize() {
        def EXCHANGE_NAME = "BROADCAST"
        def QUEUE_NAME = "zstack.newui.api.event." + UuidService.getUuid()

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(grailsApplication.config.getProperty('rabbitmq.host'));
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        channel.queueDeclare(QUEUE_NAME, false, false, true, null);
        //String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "key.event.API.API_EVENT");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                this.lastMessage = " [x] Received '" + message + "'"
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }

}
