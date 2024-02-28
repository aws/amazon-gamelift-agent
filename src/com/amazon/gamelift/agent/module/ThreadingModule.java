/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.module;

import com.amazon.gamelift.agent.logging.GameLiftAgentLogUploader;
import com.amazon.gamelift.agent.manager.ExecutorServiceManager;
import com.amazon.gamelift.agent.manager.HeartbeatSender;
import com.amazon.gamelift.agent.manager.InstanceTerminationMonitor;
import com.amazon.gamelift.agent.manager.ShutdownOrchestrator;
import com.amazon.gamelift.agent.process.GameProcessMonitor;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import dagger.Module;
import dagger.Provides;

import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Module to provide the dependencies for threading-related constructs.
 */
@Module
public class ThreadingModule {

    public static final String EXECUTOR_SERVICE_MANAGER = "ExecutorServiceManager";
    public static final String HEARTBEAT_SENDER_EXECUTOR = "HeartbeatSenderExecutorService";
    public static final String INSTANCE_TERMINATION_EXECUTOR = "InstanceTerminationMonitorExecutorService";
    public static final String SHUTDOWN_ORCHESTRATOR_EXECUTOR = "ShutdownOrchestratorExecutorService";
    public static final String GAME_PROCESS_MONITOR_EXECUTOR = "GameProcessMonitorExecutorService";
    public static final String WEBSOCKET_CONNECTION_PROVIDER_EXECUTOR = "WebSocketConnectionProviderExecutorService";
    public static final String GAMELIFT_AGENT_LOG_UPLOADER_EXECUTOR = "GameLiftAgentLogUploaderExecutorService";
    public static final String GAME_SESSION_LOGS_UPLOAD_EXECUTOR = "GameSessionLogsUploadExecutorService";

    private static final int DEFAULT_THREAD_COUNT = 1;
    private static final int GAME_SESSION_LOGS_THREAD_COUNT = 5;

    /**
     * Provides ExecutorServiceManager
     * @return
     */
    @Provides
    @Singleton
    @Named(EXECUTOR_SERVICE_MANAGER)
    public ExecutorServiceManager provideExecutorServiceManager() {
        return new ExecutorServiceManager();
    }

    /**
     * Provides HeartbeatSender ExecutorService
     * @param executorServiceManager
     * @return
     */
    @Provides
    @Singleton
    @Named(HEARTBEAT_SENDER_EXECUTOR)
    public ScheduledExecutorService provideHeartbeatSenderExecutorService(
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        final boolean setDaemon = false;
        return executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                HeartbeatSender.class.getSimpleName(), setDaemon);
    }

    /**
     * Provides InstanceTermination ExecutorService
     * @param executorServiceManager
     * @return
     */
    @Provides
    @Singleton
    @Named(INSTANCE_TERMINATION_EXECUTOR)
    public ScheduledExecutorService provideInstanceTerminationMonitorExecutorService(
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        final boolean setDaemon = true;
        return executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                InstanceTerminationMonitor.class.getSimpleName(), setDaemon);
    }

    /**
     * Provides Shutdown ExecutorService
     * @param executorServiceManager
     * @return
     */
    @Provides
    @Singleton
    @Named(SHUTDOWN_ORCHESTRATOR_EXECUTOR)
    public ScheduledExecutorService provideShutdownOrchestratorExecutorService(
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        final boolean setDaemon = false;
        return executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                ShutdownOrchestrator.class.getSimpleName(), setDaemon);
    }

    /**
     * Provides GameProcess ExecutorService
     * @param executorServiceManager
     * @return
     */
    @Provides
    @Singleton
    @Named(GAME_PROCESS_MONITOR_EXECUTOR)
    public ScheduledExecutorService provideGameProcessMonitorExecutorService(
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        final boolean setDaemon = false;
        return executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                GameProcessMonitor.class.getSimpleName(), setDaemon);
    }

    /**
     * Provides WebsocketConnection ExecutorService
     * @param executorServiceManager
     * @return
     */
    @Provides
    @Singleton
    @Named(WEBSOCKET_CONNECTION_PROVIDER_EXECUTOR)
    public ScheduledExecutorService provideWebsocketConnectionProviderExecutorService(
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        final boolean setDaemon = false;
        return executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                WebSocketConnectionProvider.class.getSimpleName(), setDaemon);
    }

    /**
     * Provide LogUploader ExecutorService
     * @param executorServiceManager
     * @return
     */
    @Provides
    @Singleton
    @Named(GAMELIFT_AGENT_LOG_UPLOADER_EXECUTOR)
    public ScheduledExecutorService provideGameLiftAgentLogUploaderExecutorService(
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        final boolean setDaemon = false;
        return executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                GameLiftAgentLogUploader.class.getSimpleName(), setDaemon);
    }

    /**
     * Provide Game Session Log Upload ExecutorService
     * @param executorServiceManager
     * @return
     */
    @Provides
    @Singleton
    @Named(GAME_SESSION_LOGS_UPLOAD_EXECUTOR)
    public ScheduledExecutorService provideGameSessionLogsUploadExecutorService(
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        final boolean setDaemon = false;
        return executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(GAME_SESSION_LOGS_THREAD_COUNT,
                GAME_SESSION_LOGS_UPLOAD_EXECUTOR, setDaemon);
    }
}
