package com.dynamic.xsd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time communication.
 * Enables STOMP protocol over WebSocket for bidirectional messaging.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for pub/sub messaging
        // /topic for broadcast (one-to-many)
        // /queue for point-to-point (one-to-one)
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint at /ws
        // Accessible at ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")  // Allow all origins for development
                .withSockJS();  // Enable SockJS fallback for browsers that don't support WebSocket

        // Also register without SockJS for native WebSocket clients
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*");
    }
}
