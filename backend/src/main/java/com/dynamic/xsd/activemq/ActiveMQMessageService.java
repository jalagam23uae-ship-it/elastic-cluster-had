package com.dynamic.xsd.activemq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending and publishing messages via ActiveMQ.
 * Supports both Queue (point-to-point) and Topic (pub/sub) patterns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveMQMessageService {

    private final JmsTemplate jmsTemplate;
    private final JmsTemplate jmsTopicTemplate;

    /**
     * Send message to a queue (point-to-point).
     */
    public void sendToQueue(String queueName, Object message) {
        log.info("Sending message to queue: {}", queueName);
        try {
            jmsTemplate.convertAndSend(queueName, message);
            log.debug("Message sent to queue {} successfully", queueName);
        } catch (Exception e) {
            log.error("Failed to send message to queue {}: {}", queueName, e.getMessage(), e);
            throw new RuntimeException("Failed to send message to queue: " + e.getMessage(), e);
        }
    }

    /**
     * Publish message to a topic (pub/sub).
     */
    public void publishToTopic(String topicName, Object message) {
        log.info("Publishing message to topic: {}", topicName);
        try {
            jmsTopicTemplate.convertAndSend(topicName, message);
            log.debug("Message published to topic {} successfully", topicName);
        } catch (Exception e) {
            log.error("Failed to publish message to topic {}: {}", topicName, e.getMessage(), e);
            throw new RuntimeException("Failed to publish to topic: " + e.getMessage(), e);
        }
    }

    /**
     * Send service operation to queue.
     */
    public void sendServiceOperation(String serviceName, String operation, Map<String, Object> data) {
        String queueName = "service." + serviceName;

        Map<String, Object> message = new HashMap<>();
        message.put("service", serviceName);
        message.put("operation", operation);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());

        sendToQueue(queueName, message);
    }

    /**
     * Publish service event to topic.
     */
    public void publishServiceEvent(String serviceName, String eventType, Map<String, Object> data) {
        String topicName = "events." + serviceName;

        Map<String, Object> event = new HashMap<>();
        event.put("service", serviceName);
        event.put("eventType", eventType);
        event.put("data", data);
        event.put("timestamp", System.currentTimeMillis());

        publishToTopic(topicName, event);
    }

    /**
     * Send create operation to service queue.
     */
    public void sendCreate(String serviceName, Map<String, Object> entity) {
        sendServiceOperation(serviceName, "CREATE", entity);
        publishServiceEvent(serviceName, "CREATED", entity);
    }

    /**
     * Send update operation to service queue.
     */
    public void sendUpdate(String serviceName, String id, Map<String, Object> entity) {
        Map<String, Object> data = new HashMap<>(entity);
        data.put("id", id);
        sendServiceOperation(serviceName, "UPDATE", data);
        publishServiceEvent(serviceName, "UPDATED", data);
    }

    /**
     * Send delete operation to service queue.
     */
    public void sendDelete(String serviceName, String id) {
        Map<String, Object> data = Map.of("id", id);
        sendServiceOperation(serviceName, "DELETE", data);
        publishServiceEvent(serviceName, "DELETED", data);
    }

    /**
     * Send get operation to service queue.
     */
    public void sendGet(String serviceName, String id) {
        Map<String, Object> data = Map.of("id", id);
        sendServiceOperation(serviceName, "GET", data);
    }

    /**
     * Send list operation to service queue.
     */
    public void sendList(String serviceName) {
        sendServiceOperation(serviceName, "LIST", Map.of());
    }

    /**
     * Get queue name for a service.
     */
    public String getServiceQueueName(String serviceName) {
        return "service." + serviceName;
    }

    /**
     * Get topic name for a service.
     */
    public String getServiceTopicName(String serviceName) {
        return "events." + serviceName;
    }

    /**
     * Get request queue name for a service.
     */
    public String getRequestQueueName(String serviceName) {
        return "request." + serviceName;
    }

    /**
     * Get response queue name for a service.
     */
    public String getResponseQueueName(String serviceName) {
        return "response." + serviceName;
    }
}
