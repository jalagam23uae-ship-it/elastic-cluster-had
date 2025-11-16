package com.dynamic.xsd.activemq;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * ActiveMQ configuration for JMS messaging.
 * Supports both Queue and Topic messaging patterns.
 */
@Configuration
@EnableJms
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url:vm://localhost?broker.persistent=false}")
    private String brokerUrl;

    @Value("${spring.activemq.user:admin}")
    private String user;

    @Value("${spring.activemq.password:admin}")
    private String password;

    /**
     * ActiveMQ Connection Factory.
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        factory.setUserName(user);
        factory.setPassword(password);
        factory.setTrustAllPackages(true);
        return factory;
    }

    /**
     * JMS Template for sending messages.
     */
    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory());
        template.setMessageConverter(jacksonJmsMessageConverter());
        return template;
    }

    /**
     * JMS Template for topics.
     */
    @Bean
    public JmsTemplate jmsTopicTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory());
        template.setMessageConverter(jacksonJmsMessageConverter());
        template.setPubSubDomain(true); // Enable topic mode
        return template;
    }

    /**
     * JMS Listener Container Factory for queues.
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setMessageConverter(jacksonJmsMessageConverter());
        factory.setConcurrency("1-10");
        return factory;
    }

    /**
     * JMS Listener Container Factory for topics.
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsTopicListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setMessageConverter(jacksonJmsMessageConverter());
        factory.setPubSubDomain(true); // Enable topic mode
        factory.setConcurrency("1-5");
        return factory;
    }

    /**
     * Jackson message converter for JSON serialization.
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}
