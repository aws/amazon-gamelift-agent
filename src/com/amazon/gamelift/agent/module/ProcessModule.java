/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.module;

import com.amazon.gamelift.agent.cache.ComputeAuthTokenCacheLoader;
import com.amazon.gamelift.agent.logging.GameSessionLogFileHelper;
import com.amazon.gamelift.agent.logging.S3FileUploader;
import com.amazon.gamelift.agent.logging.UploadGameSessionLogsCallableFactory;
import com.amazon.gamelift.agent.manager.ComputeAuthTokenManager;
import com.amazon.gamelift.agent.manager.FleetRoleCredentialsConfigurationManager;
import com.amazon.gamelift.agent.manager.RuntimeConfigurationManager;
import com.amazon.gamelift.agent.manager.StaticRuntimeConfigurationManager;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.RuntimeConfiguration;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.amazon.gamelift.agent.model.gamelift.GetComputeAuthTokenResponse;
import com.amazon.gamelift.agent.process.ProcessTerminationEventManager;
import com.amazon.gamelift.agent.websocket.SdkWebsocketEndpointProvider;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.amazon.gamelift.agent.websocket.WebSocketExceptionProvider;
import com.amazon.gamelift.agent.websocket.handlers.DefaultHandler;
import com.amazon.gamelift.agent.websocket.handlers.ForceExitProcessHandler;
import com.amazon.gamelift.agent.websocket.handlers.MessageHandler;
import com.amazon.gamelift.agent.websocket.handlers.NotifyGameSessionActivatedHandler;
import com.amazon.gamelift.agent.websocket.handlers.NotifyProcessRegisteredHandler;
import com.amazon.gamelift.agent.websocket.handlers.RefreshConnectionHandler;
import com.amazon.gamelift.agent.websocket.handlers.SendHeartbeatHandler;
import com.amazon.gamelift.agent.websocket.handlers.StartComputeTerminationHandler;
import com.amazon.gamelift.agent.client.AmazonGameLiftClientWrapper;
import com.amazon.gamelift.agent.manager.DynamicRuntimeConfigurationManager;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import dagger.Module;
import dagger.Provides;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *  Module to provide dependency injection of process-related dependencies (handlers, GameLiftAgent etc)
 */
@Module
@Slf4j
public class ProcessModule {

    /**
     * Provides object mapper
     * @return
     */
    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper()
                .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule());
    }

    /**
     * Provides ProcessTerminationEventManager
     * @param webSocketConnectionProvider
     * @return
     */
    @Provides
    @Singleton
    public ProcessTerminationEventManager provideProcessTerminationEventManager(
                final WebSocketConnectionProvider webSocketConnectionProvider) {
        return new ProcessTerminationEventManager(webSocketConnectionProvider);
    }

    /**
     * Provide message handlers
     * @param defaultHandler
     * @param startComputeTerminationHandler
     * @param sendHeartbeatHandler
     * @param forceExitProcessHandler
     * @param refreshConnectionHandler
     * @return
     */
    @Provides
    @Singleton
    public Map<String, MessageHandler<?>> provideMessageHandlers(
            final DefaultHandler defaultHandler,
            final ForceExitProcessHandler forceExitProcessHandler,
            final NotifyGameSessionActivatedHandler notifyGameSessionActivatedHandler,
            final NotifyProcessRegisteredHandler notifyProcessRegisteredHandler,
            final RefreshConnectionHandler refreshConnectionHandler,
            final StartComputeTerminationHandler startComputeTerminationHandler,
            final SendHeartbeatHandler sendHeartbeatHandler) {
        return ImmutableMap.<String, MessageHandler<?>>builder()
                .put(WebSocketActions.Default.name(), defaultHandler)
                .put(WebSocketActions.ForceExitProcess.name(), forceExitProcessHandler)
                .put(WebSocketActions.NotifyGameSessionActivated.name(), notifyGameSessionActivatedHandler)
                .put(WebSocketActions.NotifyProcessRegistered.name(), notifyProcessRegisteredHandler)
                .put(WebSocketActions.RefreshConnection.name(), refreshConnectionHandler)
                .put(WebSocketActions.SendHeartbeat.name(), sendHeartbeatHandler)
                .put(WebSocketActions.StartComputeTermination.name(), startComputeTerminationHandler)
                .build();
    }

    /**
     * Provide RuntimeConfigurationManager
     * @param webSocketConnectionProvider
     * @param defaultConfigFromCli
     * @return
     */
    @Provides
    @Singleton
    public RuntimeConfigurationManager provideRuntimeConfigurationManager(
            final WebSocketConnectionProvider webSocketConnectionProvider,
            @Nullable final RuntimeConfiguration defaultConfigFromCli) {
        // If RuntimeConfiguration is provided via CLI it will be used instead of getting the Fleet RuntimeConfiguration
        if (defaultConfigFromCli != null) {
            log.info("Overriding Fleet RuntimeConfiguration with value provided via command line arguments");
            return new StaticRuntimeConfigurationManager(defaultConfigFromCli);
        } else {
            return new DynamicRuntimeConfigurationManager(webSocketConnectionProvider);
        }
    }

    /**
     * Provide ComputeAuthTokenManager
     * @param computeAuthTokenCache
     * @return
     */
    @Provides
    @Singleton
    public ComputeAuthTokenManager provideComputeAuthTokenManager(
            LoadingCache<String, GetComputeAuthTokenResponse> computeAuthTokenCache) {
            return new ComputeAuthTokenManager(computeAuthTokenCache);
    }

    /**
     * Provide ComputeAuthTokenCache
     * @param gameLift
     * @param fleetId
     * @param computeName
     * @return
     */
    @Provides
    @Singleton
    public LoadingCache<String, GetComputeAuthTokenResponse> provideComputeAuthTokenCache(
            final AmazonGameLiftClientWrapper gameLift,
            @Named(ConfigModule.FLEET_ID) final String fleetId,
            @Named(ConfigModule.COMPUTE_NAME) final String computeName) {

        // See notes in ComputeAuthTokenManager.java regarding this duration
        final Duration cacheExpirationDuration = Duration.ofMinutes(5);
        final int maxCacheEntries = 1;

        return CacheBuilder.newBuilder()
                .maximumSize(maxCacheEntries)
                .expireAfterWrite(cacheExpirationDuration.toMillis(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build(new ComputeAuthTokenCacheLoader(gameLift, fleetId, computeName));
    }

    /**
     * Provide SDK WebsocketEndpointProvider
     * @return
     */
    @Provides
    @Singleton
    public SdkWebsocketEndpointProvider provideSdkWebsocketEndpointProvider() {
        return new SdkWebsocketEndpointProvider();
    }
    /**
     * Provide UploadGameSessionLogsCallableFactory
     * @return
     */
    @Provides
    @Singleton
    public GameSessionLogFileHelper provideGameSessionLogFileHelper(
            @Named(ConfigModule.OPERATING_SYSTEM) final OperatingSystem operatingSystem) {
        return new GameSessionLogFileHelper(operatingSystem);
    }

    /**
     * Provide UploadGameSessionLogsCallableFactory
     * @return
     */
    @Provides
    @Singleton
    public UploadGameSessionLogsCallableFactory provideUploadGameSessionLogsCallableFactory(
            @Named(ConfigModule.GAME_SESSION_LOG_BUCKET) @Nullable final String gameSessionLogBucket,
            @Named(ConfigModule.FLEET_ID) final String fleetId,
            @Named(ConfigModule.COMPUTE_NAME) final String computeName,
            final S3FileUploader s3FileUploader,
            final GameSessionLogFileHelper gameSessionLogFileHelper) {
        return new UploadGameSessionLogsCallableFactory(gameSessionLogBucket, fleetId, computeName, s3FileUploader,
                gameSessionLogFileHelper);
    }

    /**
     * Provide WebSocketExceptionProvider
     * @return
     */
    @Provides
    @Singleton
    public WebSocketExceptionProvider provideWebSocketExceptionProvider(final ObjectMapper objectMapper) {
        return new WebSocketExceptionProvider(objectMapper);
    }

    /**
     * Provide FleetRoleCredentialsConfigurationManager
     * @param webSocketConnectionProvider
     * @return
     */
    @Provides
    @Singleton
    public FleetRoleCredentialsConfigurationManager provideFleetRoleCredentialsConfigurationManager(
            final WebSocketConnectionProvider webSocketConnectionProvider) {
        return new FleetRoleCredentialsConfigurationManager(webSocketConnectionProvider);
    }
}
