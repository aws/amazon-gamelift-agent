/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EcsMetadataReader {
    public static final String ECS_METADATA_ENDPOINT_V4 = "ECS_CONTAINER_METADATA_URI_V4";
    public static final String ECS_METADATA_ENDPOINT_V4_PATH_TASK = "/task";
    public static final String HTTP_REQUEST_HEADER_ACCEPT = "accept";
    public static final String HTTP_REQUEST_HEADER_APPLICATION_JSON = "application/json";
    public static final String CONTAINER_ARN_FIELD = "ContainerARN";
    public static final String TASK_ARN_FIELD = "TaskARN";
    public static final String ARN_DELIMITER = "/";

    private final Gson gson;
    private final HttpClient httpClient;
    private final SystemEnvironmentProvider systemEnvironmentProvider;

    /**
     * Constructor for EcsMetadataReader
     * @param gson
     * @param httpClient
     * @param systemEnvironmentProvider
     */
    @Inject
    public EcsMetadataReader(final Gson gson,
                             final HttpClient httpClient,
                             final SystemEnvironmentProvider systemEnvironmentProvider) {
        this.gson = gson;
        this.httpClient = httpClient;
        this.systemEnvironmentProvider = systemEnvironmentProvider;
    }

    /**
     * Get ContainerId from ECS container metadata endpoint.
     * Example ContainerARN: arn:aws:ecs:us-west-2:111122223333:container/0206b271-b33f-47ab-86c6-a0ba208a70a9
     *
     * @return String representing ContainerId.
     */
    public String getContainerId() {
        final HttpRequest request = HttpRequest.newBuilder(
                        URI.create(systemEnvironmentProvider.getenv(ECS_METADATA_ENDPOINT_V4)))
                .header(HTTP_REQUEST_HEADER_ACCEPT, HTTP_REQUEST_HEADER_APPLICATION_JSON)
                .build();
        final HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return parseIdFromJsonField(response.body(), CONTAINER_ARN_FIELD);
    }

    /**
     * Get TaskId from ECS container metadata endpoint.
     * Example TaskARN: arn:aws:ecs:us-west-2:111122223333:task/default/158d1c8083dd49d6b527399fd6414f5c
     *
     * @return String representing TaskId.
     */
    public String getTaskId() {
        final HttpRequest request = HttpRequest.newBuilder(
                        URI.create(systemEnvironmentProvider.getenv(ECS_METADATA_ENDPOINT_V4) + ECS_METADATA_ENDPOINT_V4_PATH_TASK))
                .header(HTTP_REQUEST_HEADER_ACCEPT, HTTP_REQUEST_HEADER_APPLICATION_JSON)
                .build();
        final HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return parseIdFromJsonField(response.body(), TASK_ARN_FIELD);
    }

    private String parseIdFromJsonField(final String jsonBody, final String fieldName) {
        final JsonObject json = gson.fromJson(jsonBody, JsonObject.class);
        final String[] splitTaskArn = json.get(fieldName).toString().split(ARN_DELIMITER);
        final String untrimmedId = splitTaskArn[splitTaskArn.length - 1];
        return untrimmedId.substring(0, untrimmedId.length() - 1);
    }
}
