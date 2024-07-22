/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.cli;

import com.amazon.gamelift.agent.component.DaggerCliComponent;
import com.amazon.gamelift.agent.model.AgentArgs;
import com.amazon.gamelift.agent.component.CliComponent;
import com.amazon.gamelift.agent.model.constants.GameLiftCredentials;
import com.amazon.gamelift.agent.utils.SystemEnvironmentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.amazon.gamelift.agent.cli.AgentCliParser.COMPUTE_TYPE_CONTAINER;
import static com.amazon.gamelift.agent.cli.AgentCliParser.GAMELIFT_COMPUTE_NAME;
import static com.amazon.gamelift.agent.cli.AgentCliParser.GAMELIFT_COMPUTE_TYPE;
import static com.amazon.gamelift.agent.cli.AgentCliParser.GAMELIFT_ENDPOINT;
import static com.amazon.gamelift.agent.cli.AgentCliParser.GAMELIFT_FLEET_ID;
import static com.amazon.gamelift.agent.cli.AgentCliParser.GAMELIFT_LOCATION;
import static com.amazon.gamelift.agent.cli.AgentCliParser.GAMELIFT_REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentCliParserTest {
    private static final String LAUNCH_PATH_TEST = "test";
    private static final String RUNTIME_CONFIG_TEST_JSON =
            "{\"ServerProcesses\": [{\"LaunchPath\": \"" + LAUNCH_PATH_TEST + "\","
                    + "\"ConcurrentExecutions\" : \"1\"}]}";
    private static final String FLEET_ID_FROM_ENVIRONMENT_VARIABLE = "fleet-test-option";
    private static final String FLEET_ID_FROM_CLI_OPTION = "fleet-test-1234";
    private static final String COMPUTE_NAME_TEST = "i-1234567";
    private static final String GAMELIFT_ENDPOINT_OVERRIDE = "http://glendpointoverride:25565";
    private static final String GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT = "http://GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT";
    private static final String REGION_TEST = "us-west-2";
    private static final String LOCATION_TEST = "ap-south-1";
    private static final String CERTIFICATE_PATH_TEST = "/game/cert";
    private static final String IP_ADDRESS_TEST = "http://localhost:8080";
    private static final String DNS_NAME_TEST = "myhost.aws";
    private static final String GAME_SESSION_LOG_BUCKET = "game-session-logs";
    private static final String GAMELIFT_AGENT_LOG_BUCKET = "gamelift-agent-logs";
    private static final String COMPUTE_NAME_OPTION = "compute-name";
    private static final String GAMELIFT_CREDENTIALS_OPTION = "gamelift-credentials";
    private static final String CLI_COMPUTE_NAME_CONTAINER_FLEET_ERROR =
            "Compute Name cannot be set manually for container fleet";

    private final CliComponent cliComponent = DaggerCliComponent.create();

    @Mock private CommandLineParser mockCommandLineParser;
    @Mock private HelpFormatter mockHelpFormatter;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private SystemEnvironmentProvider mockSystemEnvironmentProvider;

    @Test
    public void GIVEN_requiredArgs_WHEN_parsing_THEN_returnValues() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        final AgentArgs parsedArgs =  parser.parse(args);

        // THEN
        assertEquals(parsedArgs.getRuntimeConfiguration().getServerProcesses().get(0).getLaunchPath(), LAUNCH_PATH_TEST);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_CLI_OPTION);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_OVERRIDE);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
        assertEquals(parsedArgs.getCertificatePath(), CERTIFICATE_PATH_TEST);
        assertEquals(parsedArgs.getIpAddress(), IP_ADDRESS_TEST);
        assertEquals(parsedArgs.getDnsName(), DNS_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftCredentials(), GameLiftCredentials.INSTANCE_PROFILE);
        assertEquals(parsedArgs.getIsContainerFleet(), Boolean.FALSE);
    }

    @Test
    public void GIVEN_blankGLC_WHEN_parsing_THEN_error() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST,
                "-glc", ""
        };
        AgentCliParser parser = cliComponent.buildCliParser();
        // WHEN

        // THEN
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));
    }

    @Test
    public void GIVEN_instance_profile_WHEN_parsing_THEN_returnValues() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST,
                "-glc", GameLiftCredentials.INSTANCE_PROFILE.getValue()
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        final AgentArgs parsedArgs =  parser.parse(args);

        // THEN
        assertEquals(parsedArgs.getRuntimeConfiguration().getServerProcesses().get(0).getLaunchPath(), LAUNCH_PATH_TEST);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_CLI_OPTION);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_OVERRIDE);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
        assertEquals(parsedArgs.getCertificatePath(), CERTIFICATE_PATH_TEST);
        assertEquals(parsedArgs.getIpAddress(), IP_ADDRESS_TEST);
        assertEquals(parsedArgs.getDnsName(), DNS_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftCredentials(), GameLiftCredentials.INSTANCE_PROFILE);
        assertEquals(parsedArgs.getIsContainerFleet(), Boolean.FALSE);
    }

    @Test
    public void GIVEN_environment_variable_WHEN_parsing_THEN_returnValues() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST,
                "-glc", GameLiftCredentials.ENVIRONMENT_VARIABLE.getValue()
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        final AgentArgs parsedArgs =  parser.parse(args);

        // THEN
        assertEquals(parsedArgs.getRuntimeConfiguration().getServerProcesses().get(0).getLaunchPath(), LAUNCH_PATH_TEST);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_CLI_OPTION);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_OVERRIDE);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
        assertEquals(parsedArgs.getCertificatePath(), CERTIFICATE_PATH_TEST);
        assertEquals(parsedArgs.getIpAddress(), IP_ADDRESS_TEST);
        assertEquals(parsedArgs.getDnsName(), DNS_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftCredentials(), GameLiftCredentials.ENVIRONMENT_VARIABLE);
        assertEquals(parsedArgs.getIsContainerFleet(), Boolean.FALSE);
    }

    @Test
    public void GIVEN_container_WHEN_parsing_THEN_returnValues() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST,
                "-glc", GameLiftCredentials.CONTAINER.getValue()
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        final AgentArgs parsedArgs =  parser.parse(args);

        // THEN
        assertEquals(parsedArgs.getRuntimeConfiguration().getServerProcesses().get(0).getLaunchPath(), LAUNCH_PATH_TEST);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_CLI_OPTION);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_OVERRIDE);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
        assertEquals(parsedArgs.getCertificatePath(), CERTIFICATE_PATH_TEST);
        assertEquals(parsedArgs.getIpAddress(), IP_ADDRESS_TEST);
        assertEquals(parsedArgs.getDnsName(), DNS_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftCredentials(), GameLiftCredentials.CONTAINER);
    }

    @Test
    public void GIVEN_fleetIdEnvArgNotAvailable_WHEN_parsingAndNotOnCliInput_THEN_illegalArgumentException() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));

        // THEN IllegalArgumentException
    }

    @Test
    public void GIVEN_computeNameEnvArgNotAvailable_WHEN_parsingAndNotOnCliInput_THEN_illegalArgumentException() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));

        // THEN IllegalArgumentException
    }

    @Test
    public void GIVEN_regionEnvArgNotAvailable_WHEN_parsingAndNotOnCliInput_THEN_illegalArgumentException() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));

        // THEN IllegalArgumentException
    }

    @Test
    public void GIVEN_locationEnvArgNotAvailable_WHEN_parsingAndNotOnCliInput_THEN_illegalArgumentException() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));

        // THEN IllegalArgumentException
    }

    @Test
    public void GIVEN_requiredArgsWithTestConfiguration_WHEN_parsing_THEN_returnValues() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        final AgentArgs parsedArgs =  parser.parse(args);

        // THEN
        assertEquals(parsedArgs.getRuntimeConfiguration().getServerProcesses().get(0).getLaunchPath(), LAUNCH_PATH_TEST);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_CLI_OPTION);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_OVERRIDE);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
        assertEquals(parsedArgs.getCertificatePath(), CERTIFICATE_PATH_TEST);
        assertEquals(parsedArgs.getIpAddress(), IP_ADDRESS_TEST);
        assertEquals(parsedArgs.getDnsName(), DNS_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftCredentials(), GameLiftCredentials.INSTANCE_PROFILE);
    }

    @Test
    public void GIVEN_s3LogBuckets_WHEN_parsing_THEN_returnValues() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-gleo", GAMELIFT_ENDPOINT_OVERRIDE,
                "-r", REGION_TEST,
                "-loc", LOCATION_TEST,
                "-gslb", GAME_SESSION_LOG_BUCKET,
                "-galb", GAMELIFT_AGENT_LOG_BUCKET
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        final AgentArgs parsedArgs =  parser.parse(args);

        // THEN
        assertEquals(parsedArgs.getRuntimeConfiguration().getServerProcesses().get(0).getLaunchPath(), LAUNCH_PATH_TEST);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_CLI_OPTION);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_OVERRIDE);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
        assertEquals(parsedArgs.getGameSessionLogBucket(), GAME_SESSION_LOG_BUCKET);
        assertEquals(parsedArgs.getAgentLogBucket(), GAMELIFT_AGENT_LOG_BUCKET);
        assertEquals(parsedArgs.getGameLiftCredentials(), GameLiftCredentials.INSTANCE_PROFILE);
    }

    @Test
    public void GIVEN_missingRequiredArg_WHEN_parsing_THEN_failure() {
        // GIVEN
        final String[] args = new String[0];
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));
    }

    @Test
    public void GIVEN_malformedJsonForRuntimeConfig_WHEN_parsing_THEN_failure() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", "{\"Not Proper JSON\"[]}",
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-r", REGION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));
    }

    @Test
    public void GIVEN_missingRuntimeConfigRequiredFields_WHEN_parsing_THEN_failure() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", "{\"GameSessionActivationTimeoutSeconds\": 1}",
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-r", REGION_TEST,
                "-cp", CERTIFICATE_PATH_TEST,
                "-ip", IP_ADDRESS_TEST,
                "-dns", DNS_NAME_TEST
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));
    }

    @Test
    public void GIVEN_helpOption_WHEN_parsing_THEN_systemExit() {
        // GIVEN
        final String[] args = new String[]{"--help"};
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        try {
            parser.parse(args);
            fail("HelpRequested Exception should be thrown when requesting help.");
        } catch (final HelpRequestedException e) {
            // THEN
        }
    }

    @Test
    public void GIVEN_emptyOptionalArgs_WHEN_parsing_THEN_returnValuesWithNulls() {
        // GIVEN
        final String[] args = new String[]{
                "-rc", RUNTIME_CONFIG_TEST_JSON,
                "-f", FLEET_ID_FROM_CLI_OPTION,
                "-c", COMPUTE_NAME_TEST,
                "-r", REGION_TEST,
                "-loc", "",
                "-gleo", "",
                "-cp", "",
                "-ip", "",
                "-dns", "",
                "-gslb", "",
                "-galb", ""
        };
        final AgentCliParser parser = cliComponent.buildCliParser();

        // WHEN
        final AgentArgs parsedArgs =  parser.parse(args);

        // THEN
        assertEquals(parsedArgs.getRuntimeConfiguration().getServerProcesses().get(0).getLaunchPath(), LAUNCH_PATH_TEST);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_CLI_OPTION);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
        assertNull(parsedArgs.getGameLiftEndpointOverride());
        assertNull(parsedArgs.getCertificatePath());
        assertNull(parsedArgs.getIpAddress());
        assertNull(parsedArgs.getDnsName());
        assertEquals(parsedArgs.getGameLiftCredentials(), GameLiftCredentials.INSTANCE_PROFILE);
        assertNull(parsedArgs.getGameSessionLogBucket());
        assertNull(parsedArgs.getAgentLogPath());
        assertEquals(parsedArgs.getIsContainerFleet(), Boolean.FALSE);
    }

    @Test
    public void GIVEN_systemEnvironmentVariables_WHEN_parsing_THEN_returnSystemEnvironmentValues() throws ParseException {
        // GIVEN
        final CommandLine mockCommandLine = mock(CommandLine.class);
        final AgentCliParser parser = new AgentCliParser(
                mockCommandLineParser, mockHelpFormatter, mockObjectMapper, mockSystemEnvironmentProvider);
        when(mockCommandLineParser.parse(any(), any()))
                .thenReturn(mockCommandLine);
        when(mockCommandLine.getOptionValue(anyString())).thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_ENDPOINT)))
                .thenReturn(GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_FLEET_ID)))
                .thenReturn(FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_TYPE)))
                .thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_NAME)))
                .thenReturn(COMPUTE_NAME_TEST);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_LOCATION)))
                .thenReturn(LOCATION_TEST);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_REGION)))
                .thenReturn(REGION_TEST);

        // WHEN
        final AgentArgs parsedArgs = parser.parse(new String[]{});

        // THEN
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getLocation(), LOCATION_TEST);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
    }

    @Test
    public void GIVEN_environmentVariablesNoComputeNameWithContainerFleet_WHEN_parsing_THEN_returnNullComputeName() throws ParseException {
        // GIVEN
        final CommandLine mockCommandLine = mock(CommandLine.class);
        final AgentCliParser parser = new AgentCliParser(
                mockCommandLineParser, mockHelpFormatter, mockObjectMapper, mockSystemEnvironmentProvider);
        when(mockCommandLineParser.parse(any(), any()))
                .thenReturn(mockCommandLine);
        when(mockCommandLine.getOptionValue(anyString())).thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_ENDPOINT)))
                .thenReturn(GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_FLEET_ID)))
                .thenReturn(FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_TYPE)))
                .thenReturn(COMPUTE_TYPE_CONTAINER);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_NAME)))
                .thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_LOCATION)))
                .thenReturn(LOCATION_TEST);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_REGION)))
                .thenReturn(REGION_TEST);

        // WHEN
        final AgentArgs parsedArgs = parser.parse(new String[]{});

        // THEN
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        assertEquals(parsedArgs.getLocation(), LOCATION_TEST);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
        assertNull(parsedArgs.getComputeName());
    }

    @Test
    public void GIVEN_environmentVariablesNoLocation_WHEN_parsing_THEN_returnRegionAsLocation() throws ParseException {
        // GIVEN
        final CommandLine mockCommandLine = mock(CommandLine.class);
        final AgentCliParser parser = new AgentCliParser(
                mockCommandLineParser, mockHelpFormatter, mockObjectMapper, mockSystemEnvironmentProvider);
        when(mockCommandLineParser.parse(any(), any()))
                .thenReturn(mockCommandLine);
        when(mockCommandLine.getOptionValue(anyString())).thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_ENDPOINT)))
                .thenReturn(GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_FLEET_ID)))
                .thenReturn(FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_TYPE)))
                .thenReturn(COMPUTE_TYPE_CONTAINER);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_NAME)))
                .thenReturn(COMPUTE_NAME_TEST);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_LOCATION)))
                .thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_REGION)))
                .thenReturn(REGION_TEST);

        // WHEN
        final AgentArgs parsedArgs = parser.parse(new String[]{});

        // THEN
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getLocation(), REGION_TEST);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
    }

    @Test
    public void GIVEN_cliComputeNameWithContainerFleet_WHEN_parsing_THEN_failure() throws ParseException {
        // GIVEN
        final CommandLine mockCommandLine = mock(CommandLine.class);
        final AgentCliParser parser = new AgentCliParser(
                mockCommandLineParser, mockHelpFormatter, mockObjectMapper, mockSystemEnvironmentProvider);
        when(mockCommandLineParser.parse(any(), any()))
                .thenReturn(mockCommandLine);
        when(mockCommandLine.hasOption(GAMELIFT_CREDENTIALS_OPTION))
                .thenReturn(false);
        when(mockCommandLine.hasOption(COMPUTE_NAME_OPTION))
                .thenReturn(true);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_ENDPOINT)))
                .thenReturn(GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_FLEET_ID)))
                .thenReturn(FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_NAME)))
                .thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_TYPE)))
                .thenReturn(COMPUTE_TYPE_CONTAINER);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_LOCATION)))
                .thenReturn(LOCATION_TEST);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_REGION)))
                .thenReturn(REGION_TEST);

        // WHEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[]{}));

        // THEN
        assertEquals(thrown.getMessage(), CLI_COMPUTE_NAME_CONTAINER_FLEET_ERROR);
    }
    @Test
    public void GIVEN_webSocketOverride_WHEN_parsing_THEN_returnSystemEnvironmentValues() throws ParseException {
        // GIVEN
        final CommandLine mockCommandLine = mock(CommandLine.class);
        final AgentCliParser parser = new AgentCliParser(
                mockCommandLineParser, mockHelpFormatter, mockObjectMapper, mockSystemEnvironmentProvider);
        when(mockCommandLineParser.parse(any(), any()))
                .thenReturn(mockCommandLine);
        when(mockCommandLine.getOptionValue(anyString())).thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_ENDPOINT)))
                .thenReturn(GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_FLEET_ID)))
                .thenReturn(FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_TYPE)))
                .thenReturn(null);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_COMPUTE_NAME)))
                .thenReturn(COMPUTE_NAME_TEST);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_LOCATION)))
                .thenReturn(LOCATION_TEST);
        when(mockSystemEnvironmentProvider.getenv(eq(GAMELIFT_REGION)))
                .thenReturn(REGION_TEST);

        // WHEN
        final AgentArgs parsedArgs = parser.parse(new String[]{});

        // THEN
        assertEquals(parsedArgs.getGameLiftEndpointOverride(), GAMELIFT_ENDPOINT_SYSTEM_ENVIRONMENT);
        assertEquals(parsedArgs.getFleetId(), FLEET_ID_FROM_ENVIRONMENT_VARIABLE);
        assertEquals(parsedArgs.getComputeName(), COMPUTE_NAME_TEST);
        assertEquals(parsedArgs.getLocation(), LOCATION_TEST);
        assertEquals(parsedArgs.getRegion(), REGION_TEST);
    }
}
