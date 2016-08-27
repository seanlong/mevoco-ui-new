package zstackui

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import com.rabbitmq.client.*;


class HelloController {

    private static final String EXCHANGE_NAME = "BROADCAST";
    private static final String QUEUE_NAME = "hello.zstack.message";

    def index() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(grailsApplication.config.getProperty('rabbitmq.host'));
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        //String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "key.event.API.API_EVENT");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);

        render(view: "index")
    }

    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    protected String hello(String world) {
        println "hello from controller, ${world}!"
        return "hello from controller, ${world}!"
    }
}
