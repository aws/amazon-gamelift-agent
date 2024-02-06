## GameLiftAgent
GameLiftAgent is a Java application that is used to launch game server processes on Amazon GameLift fleets.

This application, as provided, registers a compute resource for an existing Amazon GameLift fleet using the RegisterCompute
API. It also calls GetComputeAuthToken to fetch an authorization token for the compute resource and uses it to make
a web socket connection to the Amazon GameLift service.

## Quick Start
### JDK Version
GameLiftAgent was built with Java 17 and will require (at least) this version to compile. 
Check your java version.
```
java -version
```
If the java version is not showing Java 17, then you will have to install Java 17.

### Build GameLiftAgent using Maven

1. Navigate to the GameLiftAgent package root (directory including `pom.xml` file)
2. Execute the following to download dependencies, compile the project and generate a standalone jar using Maven:
```
mvn clean compile assembly:single
```
If this is successfully, then GameLiftAgent-1.0-jar-with-dependencies.jar will become available in the following path:

```
ls ./target/GameLiftAgent-1.0-jar-with-dependencies.jar
```

### Run the application/jar
1. The standalone jar will be located in `./target/` and can be launched with a command such as the following:
```
java -jar ./target/GameLiftAgent-1.0-jar-with-dependencies.jar <Command Line Options>
```

### Command Line Options
1. `certificate-path` / `cp`
   1. Optional - path to TLS certificate on compute resource. The path and certificate are not validated by Amazon GameLift. 
1. `compute-name` / `c`
   1. Required - A descriptive label that is associated with the compute resource registered to your fleet.
   1. May also be provided using environment variable `GAMELIFT_COMPUTE_NAME` instead of specifying as a command line option.
   1. For managed Amazon GameLift, this is set by Amazon GameLift to environment variable `GAMELIFT_COMPUTE_NAME`.  No command line option required.
1. `dns-name` / `dns`
   1. Optional - The DNS name of the compute resource. (this option is not yet available)
   1. This option is used with Amazon GameLift Anywhere fleets only. Either `dns-name` or `ip-address` is required.
1. `fleet-id` / `f`
   1. Required - A unique identifier for the GameLift fleet on which the compute resource will be registered.
   1. May also be provided via environment variable `GAMELIFT_FLEET_ID` instead of specifying as a command line option.
   1. For managed Amazon GameLift, this is set by Amazon GameLift to environment variable `GAMELIFT_FLEET_ID`. No command line option required.
1. `gamelift-endpoint-override` / `gleo`
   1. Optional - For internal testing purposes. Using this option will likely result in errors.
1. `gamelift-credentials` / `glc`    
   1. Optional - The source of credentials, which are used by the Amazon GameLift client make the `RegisterCompute` and `GetComputeAuthToken` API calls.
   1. Options are as follows (default is `instance-profile`):
      1. `instance-profile` - Uses credentials from the IAM profile associated with the Amazon GameLift EC2 fleet instance.
      1. `container` - Uses credentials from an ECS container IAM profile. (this option is not yet available)
      1. `environment-variable` - Uses temporary IAM role credentials exported to environment variables. (this option is not yet available)   
1. `game-session-log-bucket` / `gslb`
   1. Optional - The name of an Amazon S3 bucket in the customer AWS account to upload game session logs.
   1. Using this option requires Amazon GameLift fleets to specify an `InstanceRoleArn`. The IAM role must include `s3:PutObject` permission.
   1. Using this option results in `InstanceRoleArn` credentials being fetched and cached via the web socket `GetFleetRoleCredentials` route.
1. `ip-address` / `ip`
   1. Optional - The IP address of the compute resource. (this option is not yet available)
   1. This option is used with Amazon GameLift Anywhere fleets only. Either `dns-name` or `ip-address` is required.
1. `location` / `loc`
   1. Optional -  The location where the compute resource resides.
   1. Required for Amazon GameLift Anywhere fleets. Must match the custom location registered on the fleet.
   1. For Amazon GameLift EC2 fleets, this option is set by Amazon GameLift to environment variable `GAMELIFT_REGION`. No command line option required.
1. `gamelift-agent-log-bucket` / `galb`
   1. Optional - The name of an Amazon S3 bucket in the customer AWS account to upload logs for GameLiftAgent.
   1. Using this option requires Amazon GameLift fleets to specify an `InstanceRoleArn`. The IAM role must include `s3:PutObject` permission.
   1. Using this option results in `InstanceRoleArn` credentials being fetched and cached via the web socket `GetFleetRoleCredentials` route.
1. `gamelift-agent-log-path` / `galp`
   1. Optional - The file path where GameLiftAgent logs are stored locally. During launch, parent directories are created as required for this path.
   1. Defaults are `/local/gameliftagent/logs` for Linux and `C:\\GameLiftAgent\\Logs\\` for Windows.
1. `region` / `r`
   1. Required - The AWS region used when creating GameLift fleets.
   1. May also be provided using environment variable `GAMELIFT_REGION` instead of specifying as a command line option.
   1. For managed Amazon GameLift, this is set by Amazon GameLift to environment variable `GAMELIFT_REGION`. No command line option required. 
1. `runtime-configuration` / `rc`
   1. Optional - A static RuntimeConfiguration provided as inline JSON. 
   1. For managed Amazon GameLift Fleets, RuntimeConfiguration should set when creating or updating an Amazon GameLift fleet. No command line option required.

### Example Launch Commands - Managed GameLift

#### Linux

```
java -jar /<path>/<to>/GameLiftAgent-1.0-jar-with-dependencies.jar \
  -gslb 'gameliftgamesessionlogS3bucketname' \
  -galb 'gameliftagentlogS3bucketname' \
  -galp '/local/gameliftagent/logs/' 
``` 

#### Windows

```
java -jar C:\\path\\to\\GameLiftAgent-1.0-jar-with-dependencies.jar `
   -gslb 'gameliftgamesessionlogS3bucketname' `
   -galb 'gameliftagentlogS3bucketname' `
   -galp 'C:\\GameLiftAgent\\logs\\'
```
