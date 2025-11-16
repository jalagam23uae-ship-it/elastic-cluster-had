package com.dynamic.xsd.config;

import com.dynamic.xsd.domain.entity.EndpointMapping;
import com.dynamic.xsd.domain.entity.ServiceDefinition;
import com.dynamic.xsd.domain.enums.EndpointType;
import com.dynamic.xsd.domain.enums.HttpMethod;
import com.dynamic.xsd.repository.EndpointMappingRepository;
import com.dynamic.xsd.repository.ServiceDefinitionRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic OpenAPI Configuration.
 * Automatically generates Swagger documentation for dynamically deployed services.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DynamicOpenApiConfig {

    private final ServiceDefinitionRepository serviceRepository;
    private final EndpointMappingRepository endpointRepository;

    @Bean
    public OpenApiCustomizer dynamicEndpointsCustomizer() {
        return openApi -> {
            try {
                log.info("Dynamically generating OpenAPI documentation for deployed services");
                generateOpenApiDocs(openApi);
                log.info("OpenAPI documentation generation complete");
            } catch (Exception e) {
                log.error("Error generating dynamic OpenAPI documentation", e);
            }
        };
    }

    @Transactional(readOnly = true)
    public void generateOpenApiDocs(OpenAPI openApi) {
        // Get all deployed services
        List<ServiceDefinition> services = serviceRepository.findByStatus(ServiceDefinition.ServiceStatus.DEPLOYED);

        log.info("Found {} deployed services", services.size());

        for (ServiceDefinition service : services) {
            // Add tag for this service
            Tag serviceTag = new Tag()
                .name("Dynamic Service: " + service.getServiceName())
                .description("Dynamically generated endpoints for " + service.getServiceName() +
                           " (v" + service.getVersion() + ")");

            if (openApi.getTags() == null) {
                openApi.setTags(new java.util.ArrayList<>());
            }
            openApi.getTags().add(serviceTag);

            // Get all endpoints for this service
            List<EndpointMapping> endpoints = endpointRepository.findByServiceDefinition(service);

            log.info("Service {} has {} endpoints", service.getServiceName(), endpoints.size());

            for (EndpointMapping endpoint : endpoints) {
                if (endpoint.getActive() == null || !endpoint.getActive()) {
                    continue;
                }

                addEndpointToOpenApi(openApi, service, endpoint);
            }
        }
    }

    private void addEndpointToOpenApi(OpenAPI openApi, ServiceDefinition service, EndpointMapping endpoint) {
        if (openApi.getPaths() == null) {
            openApi.setPaths(new io.swagger.v3.oas.models.Paths());
        }

        String path = endpoint.getPath();
        PathItem pathItem = openApi.getPaths().get(path);

        if (pathItem == null) {
            pathItem = new PathItem();
            openApi.getPaths().addPathItem(path, pathItem);
        }

        if (endpoint.getEndpointType() == EndpointType.REST) {
            addRestOperation(pathItem, service, endpoint);
        } else if (endpoint.getEndpointType() == EndpointType.SOAP) {
            addSoapOperation(pathItem, service, endpoint);
        }

        log.debug("Added endpoint: {} {} for service {}",
            endpoint.getHttpMethod(), path, service.getServiceName());
    }

    private void addRestOperation(PathItem pathItem, ServiceDefinition service, EndpointMapping endpoint) {
        Operation operation = new Operation()
            .summary(getOperationSummary(endpoint))
            .description(getOperationDescription(service, endpoint))
            .addTagsItem("Dynamic Service: " + service.getServiceName())
            .operationId(service.getServiceName() + "_" + endpoint.getHttpMethod() + "_" +
                        endpoint.getOperationName());

        // Add path parameters if present
        if (endpoint.getPath().contains("{id}")) {
            Parameter idParam = new Parameter()
                .in("path")
                .name("id")
                .required(true)
                .description("Resource identifier")
                .schema(new Schema<>().type("string"));
            operation.addParametersItem(idParam);
        }

        // Add request body for POST/PUT
        if (endpoint.getHttpMethod() == HttpMethod.POST || endpoint.getHttpMethod() == HttpMethod.PUT) {
            RequestBody requestBody = new RequestBody()
                .description("Request payload")
                .required(true)
                .content(new Content()
                    .addMediaType("application/json", new MediaType()
                        .schema(new Schema<>()
                            .type("object")
                            .description("Dynamic object based on XSD schema")
                        )
                    )
                    .addMediaType("application/xml", new MediaType()
                        .schema(new Schema<>()
                            .type("object")
                            .description("Dynamic object based on XSD schema")
                        )
                    )
                );
            operation.setRequestBody(requestBody);
        }

        // Add responses
        ApiResponses responses = new ApiResponses();

        ApiResponse successResponse = new ApiResponse()
            .description("Successful operation")
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>()
                        .type("object")
                        .description("Response based on XSD schema")
                    )
                )
            );

        responses.addApiResponse("200", successResponse);

        ApiResponse errorResponse = new ApiResponse()
            .description("Error occurred")
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>()
                        .type("object")
                        .properties(Map.of(
                            "error", new Schema<>().type("string"),
                            "message", new Schema<>().type("string"),
                            "timestamp", new Schema<>().type("string").format("date-time")
                        ))
                    )
                )
            );

        responses.addApiResponse("400", errorResponse);
        responses.addApiResponse("404", errorResponse);
        responses.addApiResponse("500", errorResponse);

        operation.setResponses(responses);

        // Set operation on path item based on HTTP method
        switch (endpoint.getHttpMethod()) {
            case GET -> pathItem.setGet(operation);
            case POST -> pathItem.setPost(operation);
            case PUT -> pathItem.setPut(operation);
            case DELETE -> pathItem.setDelete(operation);
            case PATCH -> pathItem.setPatch(operation);
            default -> log.warn("Unsupported HTTP method: {}", endpoint.getHttpMethod());
        }
    }

    private void addSoapOperation(PathItem pathItem, ServiceDefinition service, EndpointMapping endpoint) {
        Operation operation = new Operation()
            .summary("SOAP Endpoint: " + endpoint.getOperationName())
            .description("SOAP web service endpoint for " + service.getServiceName() +
                       "\n\nWSDL available at: " + endpoint.getPath() + "?wsdl")
            .addTagsItem("Dynamic Service: " + service.getServiceName())
            .operationId(service.getServiceName() + "_SOAP_" + endpoint.getOperationName());

        // SOAP request body
        RequestBody requestBody = new RequestBody()
            .description("SOAP Envelope")
            .required(true)
            .content(new Content()
                .addMediaType("text/xml", new MediaType()
                    .schema(new Schema<>()
                        .type("string")
                        .description("SOAP XML envelope")
                    )
                )
            );
        operation.setRequestBody(requestBody);

        // SOAP responses
        ApiResponses responses = new ApiResponses();

        ApiResponse successResponse = new ApiResponse()
            .description("SOAP response")
            .content(new Content()
                .addMediaType("text/xml", new MediaType()
                    .schema(new Schema<>()
                        .type("string")
                        .description("SOAP XML response")
                    )
                )
            );

        responses.addApiResponse("200", successResponse);

        ApiResponse faultResponse = new ApiResponse()
            .description("SOAP fault")
            .content(new Content()
                .addMediaType("text/xml", new MediaType()
                    .schema(new Schema<>()
                        .type("string")
                        .description("SOAP fault XML")
                    )
                )
            );

        responses.addApiResponse("500", faultResponse);

        operation.setResponses(responses);

        // SOAP always uses POST
        pathItem.setPost(operation);
    }

    private String getOperationSummary(EndpointMapping endpoint) {
        return switch (endpoint.getHttpMethod()) {
            case GET -> endpoint.getPath().contains("{id}") ?
                       "Get single resource" : "Get all resources";
            case POST -> "Create new resource";
            case PUT -> "Update resource";
            case DELETE -> "Delete resource";
            default -> "Operation";
        };
    }

    private String getOperationDescription(ServiceDefinition service, EndpointMapping endpoint) {
        StringBuilder desc = new StringBuilder();
        desc.append("Dynamically generated endpoint for service: **")
            .append(service.getServiceName())
            .append("** (version ").append(service.getVersion()).append(")\n\n");

        // Get namespace from schema metadata
        if (service.getSchemaMetadata() != null && service.getSchemaMetadata().getNamespace() != null) {
            desc.append("**XSD Namespace:** ").append(service.getSchemaMetadata().getNamespace()).append("\n\n");
        }

        if (endpoint.getDescription() != null && !endpoint.getDescription().isEmpty()) {
            desc.append(endpoint.getDescription());
        } else {
            desc.append("This endpoint was automatically generated from the XSD schema.");
        }

        return desc.toString();
    }
}
