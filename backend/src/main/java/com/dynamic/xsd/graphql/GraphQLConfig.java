package com.dynamic.xsd.graphql;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL configuration for custom scalars and runtime wiring.
 */
@Configuration
public class GraphQLConfig {

    /**
     * Configure custom scalars for GraphQL.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Json)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.Object);
    }
}
