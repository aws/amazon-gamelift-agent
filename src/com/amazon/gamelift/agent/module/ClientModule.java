/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.module;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.PredefinedClientConfigurations;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.gamelift.AmazonGameLift;
import com.amazonaws.services.gamelift.AmazonGameLiftClientBuilder;
import dagger.Module;
import dagger.Provides;

import java.net.http.HttpClient;
import java.net.http.WebSocket;

import javax.annotation.Nullable;
import javax.inject.Named;

/**
 * Module to provide the dependencies for SDK clients.
 */
@Module
public class ClientModule {

    /**
     * Provides AmazonGameLift client
     * @param region
     * @param gameLiftEndpointOverride
     * @param credentialsProvider
     * @return
     */
    @Provides
    public AmazonGameLift provideAmazonGameLift(
            @Named(ConfigModule.REGION) final String region,
            @Named(ConfigModule.GAMELIFT_ENDPOINT_OVERRIDE) @Nullable final String gameLiftEndpointOverride,
            @Named(ConfigModule.GAMELIFT_CREDENTIALS) final AWSCredentialsProvider credentialsProvider
    ) {
        final ClientConfiguration clientConfiguration = PredefinedClientConfigurations.defaultConfig();

        final AmazonGameLiftClientBuilder amazonGameLiftClientBuilder = AmazonGameLiftClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withClientConfiguration(clientConfiguration);

        if (gameLiftEndpointOverride == null) {
            amazonGameLiftClientBuilder.setRegion(region);
        } else {
            amazonGameLiftClientBuilder.setEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(gameLiftEndpointOverride, region)
            );
        }

        return amazonGameLiftClientBuilder.build();
    }

    /**
     * Provides Websocket builder
     * @return
     */
    @Provides
    public WebSocket.Builder provideWebSocketBuilder() {
        return HttpClient.newBuilder().build().newWebSocketBuilder();
    }
}
