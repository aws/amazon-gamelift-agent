/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.cli;

import com.amazon.gamelift.agent.model.AgentArgs;
import com.amazon.gamelift.agent.model.RuntimeConfiguration;
import com.amazon.gamelift.agent.model.constants.GameLiftCredentials;
import com.amazon.gamelift.agent.utils.RealSystemEnvironmentProvider;
import com.amazon.gamelift.agent.utils.SystemEnvironmentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.ToString;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Class to provide command line parsing specific to the GameLift agent.
 */
@ToString
@AllArgsConstructor
public class AgentCliParser {
    private static final String HELP = "help";
    private static final int DESC_PAD = 5;
    private static final int LEFT_PAD = 1;
    private static final int WIDTH = 80;

    private static final String CERTIFICATE_PATH = "certificate-path";
    private static final String CERTIFICATE_PATH_SHORT = "cp";
    private static final String COMPUTE_NAME = "compute-name";
    private static final String COMPUTE_NAME_SHORT = "c";
    private static final String DNS_NAME = "dns-name";
    private static final String DNS_NAME_SHORT = "dns";

    private static final String GAMELIFT_ENDPOINT_OVERRIDE = "gamelift-endpoint-override";
    private static final String GAMELIFT_ENDPOINT_OVERRIDE_SHORT = "gleo";
    private static final String GAMELIFT_CREDENTIALS = "gamelift-credentials";
    private static final String GAMELIFT_CREDENTIALS_SHORT = "glc";
    private static final String GAME_SESSION_LOG_BUCKET = "game-session-log-bucket";
    private static final String GAME_SESSION_LOG_BUCKET_SHORT = "gslb";
    private static final String IP_ADDRESS = "ip-address";
    private static final String IP_ADDRESS_SHORT = "ip";
    private static final String LOCATION = "location";
    private static final String LOCATION_SHORT = "loc";
    private static final String GAMELIFT_AGENT_LOG_BUCKET = "gamelift-agent-log-bucket";
    private static final String GAMELIFT_AGENT_LOG_BUCKET_SHORT = "galb";
    private static final String GAMELIFT_AGENT_LOG_PATH = "gamelift-agent-log-path";
    private static final String GAMELIFT_AGENT_LOG_PATH_SHORT = "galp";
    private static final String REGION = "region";
    private static final String REGION_SHORT = "r";
    private static final String RUNTIME_CONFIGURATION_SHORT = "rc";

    private final CommandLineParser parser;
    private final HelpFormatter formatter;
    private final ObjectMapper mapper;
    private final SystemEnvironmentProvider systemEnvironmentProvider;

    public static final String CONTAINER_INPUT = "container";
    public static final String FLEET_ID_OPTION = "fleet-id";
    public static final String FLEET_ID_OPTION_SHORT = "f";
    public static final String ENVIRONMENT_VARIABLE_INPUT = "environment-variable";
    public static final String GAMELIFT_COMPUTE_NAME = "GAMELIFT_COMPUTE_NAME";
    public static final String GAMELIFT_COMPUTE_TYPE = "GAMELIFT_COMPUTE_TYPE";
    public static final String COMPUTE_TYPE_CONTAINER = "CONTAINER";
    public static final String GAMELIFT_ENDPOINT = "GAMELIFT_ENDPOINT";
    public static final String GAMELIFT_FLEET_ID = "GAMELIFT_FLEET_ID";
    public static final String GAMELIFT_LOCATION = "GAMELIFT_LOCATION";
    public static final String GAMELIFT_REGION = "GAMELIFT_REGION";
    public static final String INSTANCE_PROFILE_INPUT = "instance-profile";
    public static final String RUNTIME_CONFIGURATION_OPTION = "runtime-configuration";
    public static final String ENABLE_COMPUTE_REGISTRATION_VIA_AGENT = "GAMELIFT_ENABLE_COMPUTE_REGISTRATION_VIA_AGENT";

    /**
     * Constructor for AgentCliParser
     * NOTE: slf4j / logging is intentionally omitted from this file. Log4j is configured AFTER information is parsed
     * from the CLI input because some of it may be used when configuring log4j.
     * @param parser
     * @param formatter
     * @param mapper
     */
    @Inject
    public AgentCliParser(final CommandLineParser parser, final HelpFormatter formatter, final ObjectMapper mapper) {
        this.parser = parser;
        this.formatter = formatter;
        this.mapper = mapper;
        this.systemEnvironmentProvider = new RealSystemEnvironmentProvider();
    }

    /**
     * Parse the commandline arguments and return a GameLiftAgent args object.
     * @param args Arguments to parse
     * @return Parsed arguments object
     */
    public AgentArgs parse(final String[] args) {
        final Options options = createCommandLineOptions();
        final CommandLine commandLine = parseCommandLine(args, options);

        // RuntimeConfiguration may be explicitly provided via CLI if desired. By default, it will be fetched dynamically
        // based on the latest RuntimeConfiguration set on the Fleet
        RuntimeConfiguration runtimeConfiguration = null;
        try {
            final String runtimeConfigurationString = commandLine.getOptionValue(RUNTIME_CONFIGURATION_OPTION);
            if (runtimeConfigurationString != null) {
                runtimeConfiguration = mapper.readValue(commandLine.getOptionValue(RUNTIME_CONFIGURATION_OPTION),
                        RuntimeConfiguration.class);
            }
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error processing provided RuntimeConfiguration. Error: %s", e));
        }

        // Environment variables are prioritized over CLI parameters to ensure correct functionality with
        // GameLift-managed resources.
        final String fleetId;
        if (StringUtils.isNotBlank(systemEnvironmentProvider.getenv(GAMELIFT_FLEET_ID))) {
            // Note: This is set by GameLift for GameLift-managed resources. For GameLift Anywhere, user should either
            // configure this env variable or launch GameLiftAgent providing Fleet ID via CLI
            fleetId = systemEnvironmentProvider.getenv(GAMELIFT_FLEET_ID);
        } else {
            fleetId = commandLine.getOptionValue(FLEET_ID_OPTION);
            if (fleetId == null) {
                throw new IllegalArgumentException(
                        "Fleet ID not present in environment variables and not provided via CLI.");
            }
        }

        // GameLiftCredentials represents which credentials are used to create an Amazon GameLift client.
        // By default, GameLiftCredentials will be set to use EC2 InstanceProfileCredentials
        GameLiftCredentials gameLiftCredentials = GameLiftCredentials.INSTANCE_PROFILE;
        boolean isContainerFleet = false;
        if (commandLine.hasOption(GAMELIFT_CREDENTIALS)) {
            gameLiftCredentials = switch (commandLine.getOptionValue(GAMELIFT_CREDENTIALS)) {
                case ENVIRONMENT_VARIABLE_INPUT ->
                    // Use credentials stored in environment variables when creating an Amazon GameLift client
                    GameLiftCredentials.ENVIRONMENT_VARIABLE;
                case CONTAINER_INPUT ->
                    // Use credentials from an ECS container when creating an Amazon GameLift client
                    GameLiftCredentials.CONTAINER;
                case INSTANCE_PROFILE_INPUT ->
                    // Use credentials from an EC2 instance profile to create an Amazon GameLift client
                    GameLiftCredentials.INSTANCE_PROFILE;
                default ->
                    // Anything besides the allowed options of GAMELIFT_CREDENTIALS will be rejected.
                    throw new IllegalArgumentException(
                            String.format("%s is not a valid option. Please use one of the follow options: "
                                            + "instance-profile | environment-variable | container",
                                    commandLine.getOptionValue(GAMELIFT_CREDENTIALS)));
            };
        }
        if (COMPUTE_TYPE_CONTAINER.equals(systemEnvironmentProvider.getenv(GAMELIFT_COMPUTE_TYPE))) {
            gameLiftCredentials = GameLiftCredentials.CONTAINER;
            isContainerFleet = true;
        }

        // Environment variables are prioritized over CLI parameters to ensure correct functionality with
        // GameLift-managed resources.
        final String computeName;
        if (StringUtils.isNotBlank(systemEnvironmentProvider.getenv(GAMELIFT_COMPUTE_NAME))) {
            // Note: This is set by GameLift for GameLift-managed resources. For GameLift Anywhere user should either
            // configure this env variable or launch GameLiftAgent providing ComputeName via CLI
            computeName = systemEnvironmentProvider.getenv(GAMELIFT_COMPUTE_NAME);
        } else if (commandLine.hasOption(COMPUTE_NAME) && isContainerFleet) {
            throw new IllegalArgumentException(
                    "Compute Name cannot be set manually for container fleet");
        } else if (isContainerFleet) {
            computeName = null;
        } else {
            computeName = commandLine.getOptionValue(COMPUTE_NAME);
            if (computeName == null) {
                throw new IllegalArgumentException(
                        "Compute Name not present in environment variables and not provided via CLI.");
            }
        }

        // Environment variables are prioritized over CLI parameters to ensure correct functionality with
        // GameLift-managed resources.
        final String region;
        if (StringUtils.isNotBlank(systemEnvironmentProvider.getenv(GAMELIFT_REGION))) {
            // Note: This is set by GameLift for GameLift-managed resources. For GameLift Anywhere user should either
            // configure this env variable or launch GameLiftAgent providing region via CLI
            region = systemEnvironmentProvider.getenv(GAMELIFT_REGION);
        } else {
            region = commandLine.getOptionValue(REGION);
            if (region == null) {
                throw new IllegalArgumentException(
                        "Region not present in environment variables and not provided via CLI.");
            }
        }

        // Environment variables are prioritized over CLI parameters to ensure correct functionality with
        // GameLift-managed resources. For GameLift Anywhere this must be provided via CLI as the values for region and
        // location are not equivalent.
        final String location;
        if (StringUtils.isNotBlank(systemEnvironmentProvider.getenv(GAMELIFT_LOCATION))
                || StringUtils.isNotBlank(systemEnvironmentProvider.getenv(GAMELIFT_REGION))) {
            // Note: This is set by GameLift for GameLift-managed resources.
            final String locationFromEnvironment = systemEnvironmentProvider.getenv(GAMELIFT_LOCATION);
            final String regionFromEnvironment = systemEnvironmentProvider.getenv(GAMELIFT_REGION);
            location = (locationFromEnvironment == null) ? regionFromEnvironment : locationFromEnvironment;
        } else {
            location = commandLine.getOptionValue(LOCATION);
            if (location == null) {
                throw new IllegalArgumentException(
                        "Location not present in environment variables and not provided via CLI.");
            }
        }

        String gameLiftEndpointOverride = commandLine.getOptionValue(GAMELIFT_ENDPOINT_OVERRIDE);
        if (StringUtils.isNotBlank(systemEnvironmentProvider.getenv(GAMELIFT_ENDPOINT))) {
            gameLiftEndpointOverride = systemEnvironmentProvider.getenv(GAMELIFT_ENDPOINT);
        } else if (StringUtils.isEmpty(gameLiftEndpointOverride)) {
            gameLiftEndpointOverride = null;
        }

        final String certificatePath = getOptionValueOrNull(commandLine, CERTIFICATE_PATH);

        final String ipAddress = getOptionValueOrNull(commandLine, IP_ADDRESS);

        final String dnsName = getOptionValueOrNull(commandLine, DNS_NAME);

        final String gameSessionLogBucket = getOptionValueOrNull(commandLine, GAME_SESSION_LOG_BUCKET);

        final String gameliftAgentLogBucket = getOptionValueOrNull(commandLine, GAMELIFT_AGENT_LOG_BUCKET);

        final String gameliftAgentLogPath = getOptionValueOrNull(commandLine, GAMELIFT_AGENT_LOG_PATH);
        final Boolean enabledComputeRegistrationViaAgent = StringUtils.isNotBlank(systemEnvironmentProvider
                .getenv(ENABLE_COMPUTE_REGISTRATION_VIA_AGENT)) ? Boolean.valueOf(systemEnvironmentProvider
                .getenv(ENABLE_COMPUTE_REGISTRATION_VIA_AGENT)) : false;

        return AgentArgs.builder()
                .runtimeConfiguration(runtimeConfiguration)
                .fleetId(fleetId)
                .computeName(computeName)
                .region(region)
                .location(location)
                .gameLiftEndpointOverride(gameLiftEndpointOverride)
                .certificatePath(certificatePath)
                .ipAddress(ipAddress)
                .dnsName(dnsName)
                .gameLiftCredentials(gameLiftCredentials)
                .gameSessionLogBucket(gameSessionLogBucket)
                .agentLogBucket(gameliftAgentLogBucket)
                .agentLogPath(gameliftAgentLogPath)
                .isContainerFleet(isContainerFleet)
                .enableComputeRegistrationViaAgent(enabledComputeRegistrationViaAgent)
                .build();
    }

    /**
     * Method to add options to the cli.
     *
     * @return Options to be used by the CommandLine object
     */
    private Options createCommandLineOptions() {
        final Options options = new Options();
        options.addOption(Option.builder()
                .desc("Displays help and argument usage.")
                .longOpt(HELP)
                .build());

        options.addOption(Option.builder(RUNTIME_CONFIGURATION_SHORT)
                .desc("RuntimeConfiguration for the GameLiftAgent in json. Schema can be found here: "
                        + "https://docs.aws.amazon.com/gamelift/latest/apireference/API_RuntimeConfiguration.html")
                .hasArg()
                .longOpt(RUNTIME_CONFIGURATION_OPTION)
                .build());

        options.addOption(Option.builder(FLEET_ID_OPTION_SHORT)
                .desc("Fleet ID for the Fleet on which the GameLiftAgent is running.")
                .hasArg()
                .longOpt(FLEET_ID_OPTION)
                .build());

        options.addOption(Option.builder(COMPUTE_NAME_SHORT)
                .desc("ComputeName for the Compute on which the GameLiftAgent is running.")
                .hasArg()
                .longOpt(COMPUTE_NAME)
                .build());

        options.addOption(Option.builder(REGION_SHORT)
                .desc("Region for the Compute on which the GameLiftAgent is running.")
                .hasArg()
                .longOpt(REGION)
                .build());

        options.addOption(Option.builder(LOCATION_SHORT)
                .desc("Fleet Location for the Compute on which the GameLiftAgent is running.")
                .hasArg()
                .longOpt(LOCATION)
                .build());

        options.addOption(Option.builder(GAMELIFT_ENDPOINT_OVERRIDE_SHORT)
                .desc("Override for the Amazon GameLift endpoint with which the GameLiftAgent communicates.")
                .hasArg()
                .longOpt(GAMELIFT_ENDPOINT_OVERRIDE)
                .build());

        options.addOption(Option.builder(IP_ADDRESS_SHORT)
                .desc("IP address for the Compute on which the GameLiftAgent is running.")
                .hasArg()
                .longOpt(IP_ADDRESS)
                .build());

        options.addOption(Option.builder(CERTIFICATE_PATH_SHORT)
                .desc("Path to the certificate for the Compute on which the GameLiftAgent is running.")
                .hasArg()
                .longOpt(CERTIFICATE_PATH)
                .build());

        options.addOption(Option.builder(DNS_NAME_SHORT)
                .desc("DNS name for the Compute on which the GameLiftAgent is running.")
                .hasArg()
                .longOpt(DNS_NAME)
                .build());

        options.addOption(Option.builder(GAMELIFT_CREDENTIALS_SHORT)
                .desc("Source for credentials used to create an Amazon GameLift client.")
                .hasArg()
                .longOpt(GAMELIFT_CREDENTIALS)
                .build());

        options.addOption(Option.builder(GAME_SESSION_LOG_BUCKET_SHORT)
                .desc("S3 bucket name to upload GameSession logs to when sessions complete.")
                .hasArg()
                .longOpt(GAME_SESSION_LOG_BUCKET)
                .build());

        options.addOption(Option.builder(GAMELIFT_AGENT_LOG_BUCKET_SHORT)
                .desc("S3 bucket name to upload GameLiftAgent logs to periodically.")
                .hasArg()
                .longOpt(GAMELIFT_AGENT_LOG_BUCKET)
                .build());

        options.addOption(Option.builder(GAMELIFT_AGENT_LOG_PATH_SHORT)
                .desc("Directory path for storage of GameLiftAgent logs locally.")
                .hasArg()
                .longOpt(GAMELIFT_AGENT_LOG_PATH)
                .build());

        return options;
    }

    /**
     * Check if help option passed in.
     * @param args args passed in
     * @return if help option present
     */
    private void checkHelp(final String[] args, final Options options) {
        final Options helpOptions = new Options();
        helpOptions.addOption(Option.builder()
                .longOpt(HELP)
                .build());
        try {
            final CommandLine commandLine = parser.parse(helpOptions, args);
            if (commandLine.hasOption(HELP)) {
                printHelp(options);
                throw new HelpRequestedException("Help requested in cli.");
            }
        } catch (final HelpRequestedException e) {
            throw e;
        } catch (final Throwable t) {
            // process other exceptions when processing required args.
        }
    }

    /**
     * Parse the cli args.
     *
     * @param args CLI args
     * @param options Available cli options
     * @return CommandLine object with parsed options
     */
    private CommandLine parseCommandLine(final String[] args, final Options options) {
        checkHelp(args, options);
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (final Throwable t) {
            printHelp(options);
            throw new IllegalArgumentException(
                    String.format("Error parsing command line arguments: %s", t.getMessage()));
        }
        return commandLine;
    }

    /**
     * Print the help string.
     *
     * @param options Options to display in the help string
     */
    private void printHelp(final Options options) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        formatter.printHelp(pw,
                WIDTH,
                getExampleUsage(),
                null,
                options,
                LEFT_PAD,
                DESC_PAD,
                null);
    }

    /**
     * Example usage for the GameLiftAgent application.
     * @return Example usage string
     */
    private String getExampleUsage() {
        return String.format("GameLiftAgent "
                + "--%s '{\"ServerProcesses\": [{\"LaunchPath\": \"ls\", \"ConcurrentExecutions\": \"1\"}]}' "
                + "--%s fleet-1234-5678-0123 --%s i-0123456789 "
                + "--%s 1.2.3.4 --%s /cert --%s abc.aws "
                + "--%s %s "
                + "--%s game-session-logs --%s gamelift-agent-logs "
                + "--%s /local/gameliftagent/",
                RUNTIME_CONFIGURATION_OPTION,
                FLEET_ID_OPTION, COMPUTE_NAME,
                IP_ADDRESS, CERTIFICATE_PATH, DNS_NAME,
                GAMELIFT_CREDENTIALS, CONTAINER_INPUT,
                GAME_SESSION_LOG_BUCKET, GAMELIFT_AGENT_LOG_BUCKET,
                GAMELIFT_AGENT_LOG_PATH);
    }

    private static String getOptionValueOrNull(final CommandLine commandLine, final String optionValueKey) {
        String optionValue = commandLine.getOptionValue(optionValueKey);
        return StringUtils.isBlank(optionValue) ? null : optionValue;
    }
}
