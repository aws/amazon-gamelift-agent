/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.module;

import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.AgentArgs;
import com.amazon.gamelift.agent.model.RuntimeConfiguration;
import com.amazon.gamelift.agent.model.constants.GameLiftCredentials;
import com.amazon.gamelift.agent.utils.EcsMetadataReader;
import com.amazon.gamelift.agent.utils.RealSystemEnvironmentProvider;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.net.http.HttpClient;

@Slf4j
@Module
public class ConfigModule {

    public static final String FLEET_ID = "fleetId";
    public static final String COMPUTE_NAME = "computeName";
    public static final String REGION = "region";
    public static final String LOCATION = "location";
    public static final String GAMELIFT_ENDPOINT_OVERRIDE = "gameLiftEndpointOverride";
    public static final String GAMELIFT_AGENT_WEBSOCKET_ENDPOINT_OVERRIDE =
            "gameLiftAgentWebSocketEndpointOverride";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String CERTIFICATE_PATH = "certificatePath";
    public static final String DNS_NAME = "dnsName";
    public static final String IS_CONTAINER_FLEET = "isContainerFleet";
    public static final String OPERATING_SYSTEM = "operatingSystem";
    public static final String GAMELIFT_CREDENTIALS = "gameliftCredentials";
    public static final String GAME_SESSION_LOG_BUCKET = "gameSessionLogBucket";
    public static final String GAMELIFT_AGENT_LOG_BUCKET = "gameliftAgentLogBucket";
    public static final String GAMELIFT_AGENT_LOG_PATH = "gameliftAgentLogPath";
    public static final String GAMELIFT_AGENT_LOGS_DIRECTORY = "gameliftAgentLogDirectory";

    private final String fleetId;
    private final String computeName;
    private final String region;
    private final String location;
    private final String gameLiftEndpointOverride;
    private final String gameLiftAgentWebSocketEndpointOverride;
    private final String ipAddress;
    private final String certificatePath;
    private final String dnsName;
    private final RuntimeConfiguration runtimeConfiguration;
    private final GameLiftCredentials gameLiftCredentials;
    private final String gameSessionLogBucket;
    private final String gameliftAgentLogBucket;
    private final String gameliftAgentLogPath;
    private final String containerId;
    private final String taskId;
    private final boolean isContainerFleet;

    private final EcsMetadataReader ecsMetadataReader;

    /**
     * Setup ConfigModule
     * @param args {@code AgentArgs}
     */
    public ConfigModule(final AgentArgs args) {
        this.ecsMetadataReader = new EcsMetadataReader(
                new Gson(),
                HttpClient.newHttpClient(),
                new RealSystemEnvironmentProvider());
        this.fleetId = args.getFleetId();
        this.gameLiftEndpointOverride = args.getGameLiftEndpointOverride();
        this.gameLiftAgentWebSocketEndpointOverride = args.getGameLiftAgentWebSocketEndpointOverride();
        this.region = args.getRegion();
        this.location = args.getLocation();
        this.ipAddress = args.getIpAddress();
        this.certificatePath = args.getCertificatePath();
        this.dnsName = args.getDnsName();
        this.runtimeConfiguration = args.getRuntimeConfiguration();
        this.gameLiftCredentials = args.getGameLiftCredentials();
        this.gameSessionLogBucket = args.getGameSessionLogBucket();
        this.gameliftAgentLogBucket = args.getAgentLogBucket();
        this.gameliftAgentLogPath = args.getAgentLogPath();
        this.containerId = args.getIsContainerFleet() ? ecsMetadataReader.getContainerId() : null;
        this.taskId = args.getIsContainerFleet() ? ecsMetadataReader.getTaskId() : null;
        this.computeName =
                args.getIsContainerFleet() ? String.format("%s/%s", taskId, containerId) : args.getComputeName();
        this.isContainerFleet = args.getIsContainerFleet();
    }

    /**
     * Provides fleet id
     * @return String | Null
     */
    @Provides
    @Named(FLEET_ID)
    public String provideFleetId() {
        return fleetId;
    }

    /**
     * Provides compute id
     * @return String | Null
     */
    @Provides
    @Named(COMPUTE_NAME)
    public String provideComputeName() {
        return computeName;
    }

    /**
     * Provides region
     * @return String | Null
     */
    @Provides
    @Named(REGION)
    public String provideRegion() {
        return region;
    }

    /**
     * Provides location
     * @return String | Null
     */
    @Provides
    @Nullable
    @Named(LOCATION)
    public String providesLocation() {
        return location;
    }

    /**
     * Provides override for Amazon GameLift endpoint
     * @return String | Null
     */
    @Provides
    @Nullable
    @Named(GAMELIFT_ENDPOINT_OVERRIDE)
    public String provideGameLiftEndpointOverride() {
        return gameLiftEndpointOverride;
    }

    /**
     * Provides override for WebSocket endpoint
     * @return String | Null
     */
    @Provides
    @Nullable
    @Named(GAMELIFT_AGENT_WEBSOCKET_ENDPOINT_OVERRIDE)
    public String provideGameLiftAgentWebSocketEndpointOverride() {
        return gameLiftAgentWebSocketEndpointOverride;
    }

    /**
     * Provides IP address
     * @return String | Null
     */
    @Provides
    @Nullable
    @Named(IP_ADDRESS)
    public String provideIpAddress() {
        return ipAddress;
    }

    /**
     * Provides certificate path
     * @return String | Null
     */
    @Provides
    @Nullable
    @Named(CERTIFICATE_PATH)
    public String provideCertificatePath() {
        return certificatePath;
    }

    /**
     * Provides dns name
     * @return String | Null
     */
    @Provides
    @Nullable
    @Named(DNS_NAME)
    public String provideDnsName() {
        return dnsName;
    }

    /**
     * Provides RuntimeConfiguration
     * @return RuntimeConfiguration | Null
     */
    @Provides
    @Nullable
    public RuntimeConfiguration provideRuntimeConfig() {
        return runtimeConfiguration;
    }

    /**
     * Provides OperatingSystem
     * @return OperatingSystem | Null
     */
    @Provides
    @Named(OPERATING_SYSTEM)
    public OperatingSystem provideOperatingSystem() {
        return OperatingSystem.fromSystemOperatingSystem();
    }

    /**
     * Provides isContainerFleet
     * @return true | false
     */
    @Provides
    @Singleton
    @Named(IS_CONTAINER_FLEET)
    public boolean provideIsContainerFleet() {
        return this.isContainerFleet;
    }

    /**
     * Provides AWSCredentialsProvider for creating an Amazon GameLift Client
     * @return AWSCredentialsProvider
     */
    @Provides
    @Singleton
    @Named(GAMELIFT_CREDENTIALS)
    public AWSCredentialsProvider provideGameLiftCredentials() {
        final AWSCredentialsProvider specifiedProvider;
        if (GameLiftCredentials.INSTANCE_PROFILE.equals(gameLiftCredentials)) {
            log.info("Retrieving credentials from the instance metadata");
            specifiedProvider = InstanceProfileCredentialsProvider.getInstance();
        } else if (GameLiftCredentials.ENVIRONMENT_VARIABLE.equals(gameLiftCredentials)) {
            log.info("Retrieving credentials from environment variables");
            specifiedProvider = new EnvironmentVariableCredentialsProvider();
        } else if (GameLiftCredentials.CONTAINER.equals(gameLiftCredentials)) {
            log.info("Retrieving credentials from container credentials");
            specifiedProvider = new EC2ContainerCredentialsProviderWrapper();
        } else {
            throw new IllegalArgumentException(
                    "Credentials must be instance-profile | environment-variable | container");
        }

        try {
            // Attempt to get credentials from the specified source; if it fails, fall back to the default chain
            specifiedProvider.getCredentials();
            return specifiedProvider;
        } catch (final Exception e) {
            log.warn("Failed to retrieve credentials using the specified method. "
                    + "Falling back to default credential provider chain (env/JVM properties)");
            return DefaultAWSCredentialsProviderChain.getInstance();
        }
    }

    /**
     * Provides the S3 bucket to upload GameSession logs to or null if not used.
     * @return String | null
     */
    @Provides
    @Nullable
    @Named(GAME_SESSION_LOG_BUCKET)
    public String provideGameSessionLogBucket() {
        return gameSessionLogBucket;
    }

    /**
     * Provides the S3 bucket to upload Agent logs to or null if not used.
     * @return String | null
     */
    @Provides
    @Nullable
    @Named(GAMELIFT_AGENT_LOG_BUCKET)
    public String provideAgentLogBucket() {
        return gameliftAgentLogBucket;
    }

    /**
     * Provides GameLiftAgent Log location as a String
     * @param operatingSystem
     * @return
     */
    @Provides
    @Singleton
    @Named(GAMELIFT_AGENT_LOG_PATH)
    public String provideAgentLogPath(@Named(OPERATING_SYSTEM) final OperatingSystem operatingSystem) {
        if (StringUtils.isBlank(gameliftAgentLogPath)) {
            // When no log path provided use a GameLift-defined default based on OperatingSystem
            return operatingSystem.getAgentLogsFolder();
        } else {
            return gameliftAgentLogPath;
        }
    }

    /**
     * Provides GameLiftAgent Log location as a File
     * @param logPath
     * @return
     */
    @Provides
    @Singleton
    @Named(GAMELIFT_AGENT_LOGS_DIRECTORY)
    public File provideAgentLogDirectory(
            @Named(GAMELIFT_AGENT_LOG_PATH) final String logPath) {
        return new File(logPath);
    }
}
