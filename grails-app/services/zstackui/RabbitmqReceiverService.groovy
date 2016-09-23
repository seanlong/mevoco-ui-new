package zstackui

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.messaging.simp.SimpMessagingTemplate

import java.util.concurrent.ConcurrentHashMap

class RabbitmqReceiverService {

    SimpMessagingTemplate brokerMessagingTemplate

    private consumers = []
    // Map for messageId to username(which should be unique session Id generated by client)
    private ConcurrentHashMap<String, String> pendingMessages = new ConcurrentHashMap<>();
    private parser = new JsonSlurper()

    def addSession(String username) {
    }

    def removeSession(String username) {
    }

    def addPendingMessage(RequestMessage message, String messageId) {
        if (pendingMessages.contains(messageId))
            throw new Exception("Message already exist: " + messageId)
        pendingMessages.put(messageId, message.getUsername())
    }

    def addChannelConsumer(Channel channel) {
        def consumer = new DefaultConsumer(channel) {
            // For a single channel this may called in a thread pool but always processed serially
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8")
                println message
                def obj = parser.parseText(message)
                def messageBody = obj.values()[0]
                def bodyHeaders = messageBody["headers"]
                def messageId
                if (bodyHeaders["isReply"] == "true" && bodyHeaders["correlationId"])
                    messageId = bodyHeaders["correlationId"]
                else {
                    if (messageBody["apiId"])
                        messageId = messageBody["apiId"]
                    else
                        throw new Exception("Message not found in pending list")
                }
                def username = pendingMessages.get(messageId)
                pendingMessages.remove(messageId)
                brokerMessagingTemplate.convertAndSendToUser(username, "/queue/hello", message)
            }
        }
        consumers.add(consumer)
        return consumer
    }

}