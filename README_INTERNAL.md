## GameLiftAgent
GameLiftAgent is a simple Java application for spinning up customer game server processes created to
support GameLift Anywhere, Bring-Your-Own-AMI and Containers projects (and more). This package is responsible for
building the GameLiftAgent and generating a standalone JAR for testing. This software will ultimately be released as Open Source
to customers who will build the JAR themselves

## Quick Start
### Create a Workspace
```
brazil ws --create --name GameLiftAgent \
  --versionSet GameLiftProcessManagerExternal/development && \
  cd GameLiftAgent && \
  brazil ws --use --package GameLiftAgent && \
  cd src/GameLiftAgent
```

## Development
### General Process

NOTE: This section is likely inaccurate for GameLiftAgent - it is an import from IPM (GLIFT-20131)

1. Build the mainline branch of GameLiftAgent
1. Execute the output jar with `brazil-build run` and check logs to verify correctness
1. Make changes to desired resources
1. Execute the output jar and check logs to verify correctness
1. Post a CR with `gamelift-devs (TEAM)` as a required approver
1. Push changes to mainline and monitor deployment until it reaches the end of the pipeline

### Open Source Considerations
Open source docs have guidance for a different copyright than we have rules for in GameScaleCheckstyle:
https://w.amazon.com/bin/view/Open_Source/LicensingForGitHubProjects#HCopyrightDatesforAmazon-createdOpenSource
 ```
 /*
  * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
  */
 ```

## Test GameLiftAgent with GameLift Anywhere Container Compute

Reference this Quip document to generate the Container Image and test it on an Anywhere Fleet
before using the image to create a Container Fleet.

https://quip-amazon.com/6Sq7AoVnvNvO/Using-Containers-with-GameLift-Anywhere-Fleets

## Test GameLiftAgent with GameLift Anywhere

Note: There is currently a bug with Heartbeat logic for Anywhere (GLIFT-20132). Attempting to heartbeat with a
ComputeName that's the same as a previously terminated Anywhere Compute will result in a
reponse that the Compute is terminated and will crash GameLiftAgent. Until fixed you must either always
use new ComputeNames OR manually delete (in preprod) the corresponding TerminatedHost record.

### Set up your dev AWS Account

Until release, this testing requires adding your dev / test account ID to the Containers FAC:

https://code.amazon.com/packages/GameScaleServiceFacConfig/blobs/mainline/--/features/policies/feature-gamelift-containers.json

If you want to capture GameLiftAgent and/or GameSession logs, create 1 or 2 S3 buckets in the AWS console.
Create 2 buckets if you want GameLiftAgent and GameSession logs stored separately.
1. Navigate to the S3 Console and click Create Bucket.
1. Enter a bucket name:  `<user>-test-logs`
1. Choose AWS region:  `us-west-2`
1. Use recommended/secure options to disable ACLs, block public access, and encrypt at rest.

Create an IAM Role to use with instanceRoleCredentials in the AWS console
1. Navigate to the IAM Console and click Create Role.
1. Choose custom trust policy and use the following:
```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "Statement1",
            "Effect": "Allow",
            "Principal": {
                "Service": [
                    "gamelift.amazonaws.com",
                    "gamelift.aws.internal"
                ]
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
```
3. Click CreatePolicy to open a new dialogue, switch to the JSON tab and enter:
```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject"
            ],
            "Resource": "*"
        }
    ]
}
```
4. Continue and Name the policy:  `GameLiftAgentInstancePolicy`
5. Go back to the Create Role window, refresh, and choose GameLiftAgentInstancePolicy and continue.
6. Name the role: `GameLiftAgentInstanceRole` and click Create Role

### Create GameLift Resources in your dev AWS Account

1. Setting up a Location/Fleet only needs to be done once.  Create a custom Location with your dev AWS account:
```
aws gamelift create-location --region us-west-2 --endpoint-url https://gamelift-alpha.us-west-2.amazonaws.com --location-name custom-location
```

2. Create a GameLift Anywhere Fleet with your dev AWS account:
```
aws gamelift create-fleet --region us-west-2 --endpoint-url https://gamelift-alpha.us-west-2.amazonaws.com --name test-fleet --compute-type ANYWHERE --locations Location=custom-location
```

3. (GLIFT-20024) If not possible via Fleet creation yet, manually create a RuntimeConfiguration DDB record:

Linux:
```
{
  "fleetId": {
    "S": "fleet-<id>"
  },
  "awsAccountId": {
    "S": "<account-id>"
  },
  "gameServers": {
    "L": [
      {
        "M": {
          "concurrentExecutions": {
            "N": "1"
          },
          "launchPath": {
            "S": "/local/game/TestApplicationServer"
          },
          "parameters": {
            "S": "-p 1984 -l 15000"
          }
        }
      }
    ]
  },
  "gameSessionActivationTimeoutSeconds": {
    "N": "30"
  },
  "maxConcurrentGameSessionActivations": {
    "N": "2147483647"
  },
  "updateTime": {
    "N": "1503225032394"
  }
}
```
Windows:
```
{
  "fleetId": {
    "S": "fleet-<id>"
  },
  "awsAccountId": {
    "S": "<account-id>"
  },
  "gameServers": {
    "L": [
      {
        "M": {
          "concurrentExecutions": {
            "N": "1"
          },
          "launchPath": {
            "S": "C:\\Game\\TestApplicationServer.exe"
          },
          "parameters": {
            "S": "-p 1984 -l 15000"
          }
        }
      },
      {
        "M": {
          "concurrentExecutions": {
            "N": "1"
          },
          "launchPath": {
            "S": "C:\\Game\\TestApplicationServer.exe"
          },
          "parameters": {
            "S": "-p 4245 -l 15000"
          }
        }
      }
    ]
  },
  "gameSessionActivationTimeoutSeconds": {
    "N": "30"
  },
  "maxConcurrentGameSessionActivations": {
    "N": "2147483647"
  },
  "updateTime": {
    "N": "1503225032394"
  }
}
```

4. (GLIFT-20101) If not possible via Fleet creation yet, set an `instanceRoleArn` on the Fleet:
    1. Login to WW via Isengard:  https://isengard.amazon.com/federate?account=985706097632&role=RestrictedAccess
    1. Navigate to DDB Console, Explore Items, select Fleet table, and query your FleetId.
    1. Open the item to edit and insert:
```
"instanceRoleArn": {
  "S": "arn:aws:iam::<account-id>:role/GameLiftAgentInstanceRole"
},
```

### Set up the TestApp Locally (Linux)

1. Create a `/local/game` folder with user permissions for running the test app:
```
sudo mkdir /local/game
sudo chown <user>:amazon /local/game
sudo mkdir /local/whitewater
sudo chown <user>:amazon /local/whitewater
```

2. Copy latest Linux SDK5 test app from [GameScaleServerSdkCSharp](https://code.amazon.com/packages/GameScaleServerSdkCSharp/trees/mainline) to `/local/game`:
```
brazil ws create --name GameScaleServerSdkCSharp
cd GameScaleServerSdkCSharp
brazil ws use --p GameScaleServerSdkCSharp
cp src/GameScaleServerSdkCSharp/bin/gamelift-test-app-c\#-5.0.0-linux-net461-mono-std.zip /local/game
```

3. Unzip the test app:
```
cd /local/game
unzip gamelift-test-app-c\#-5.0.0-linux-net461-mono-std.zip
```

4. Run the following in `/local/game` to set executable permissions:
```
chmod 755 TestApplicationServer && chmod 755 install.sh && chmod 755 mono.sh
ls -als
```

### Set up the TestApp Locally (Windows)
This example was performed on a Windows dev laptop.
1. Create a `C:\Game` folder
2. Create a `C:\Whitewater` folder

3. Copy latest Windows SDK5 test app from [GameScaleServerSdkCSharp](https://code.amazon.com/packages/GameScaleServerSdkCSharp/trees/mainline) to `C:\Game`:

Set Up Workspace:
```
brazil ws create --name GameScaleServerSdkCSharp
cd GameScaleServerSdkCSharp
brazil ws use --p GameScaleServerSdkCSharp
```

SCP transfer test app to Windows machine (if necessary, such as from Cloud Desktop) or manually move zip to `C:\Game`:
```
1. Open `cmd.exe` on the Windows machine
2. SCP the SDK file:

scp <source> <destination>
scp user@cloud-desktop-address:/path/to/GameScaleServerSdkCSharp/src/GameScaleServerSdkCSharp/bin/gamelift-test-app-c#-5.0.0-windows-net461-msvc15-std.zip "C:\Game\gamelift-test-app-c#-5.0.0-windows-net461-msvc15-std.zip"
```

4. Unzip the test app in `C:\Game`

### Set up and execute GameLiftAgent (Linux)

1. Build GameLiftAgent: `brazil-build`
1. Run the following with your Fleet and desired ComputeName: `export GAMELIFT_FLEET_ID=fleet-<id> && export GAMELIFT_COMPUTE_NAME=compute-1`
1. Export dev AWS account temporary credentials to env vars: `ada cred update --account <account-id> --once --role Admin`
1. Record your IP address with the following: `ifconfig | grep inet`
1. Run this command to launch GameLiftAgent (substituting values as appropriate)
```
sudo java -jar build/GameLiftAgent-1_0.jar \
  -r us-west-2 \
  -glc environment-variable \
  -gleo 'https://gamelift-alpha.us-west-2.amazonaws.com' \
  -ip '205.251.233.50' \
  -loc 'custom-location' \
  -gslb '<user>-test-logs' \
  -galb '<user>-test-logs' \
  -galp '/local/gameliftagent/logs'
```

### Set up and execute GameLiftAgent (Windows)

1. Build GameLiftAgent: `brazil-build`
2. SCP transfer GameLiftAgent JAR to Windows machine (if necessary, such as from Cloud Desktop):
```
1. Open `cmd.exe` on the Windows machine
2. SCP the SDK file:

scp <source> <destination>.
scp user@cloud-desktop-address:/path/to/GameLiftAgent/build/GameLiftAgent/GameLiftAgent-1.0/AL2_x86_64/DEV.STD.PTHREAD/build/GameLiftAgent-1_0.jar "C:\Game\GameLiftAgent-1_0.jar"
```
3. Run the following with your Fleet and desired ComputeName in Windows PowerShell: 
```
$Env:GAMELIFT_FLEET_ID = 'fleet-<id>'; $Env:GAMELIFT_COMPUTE_NAME = '<compute-name>';`
```
4. Export dev AWS account PowerShell temporary credentials to env vars on Windows machine (fetch from Isengard)
5. Obtain your IP address with the following in Windows PowerShell: 
```
Get-NetIPAddress
```
6. Run this command to launch GameLiftAgent in Windows PowerShell (substituting values as appropriate)
```
java -jar GameLiftAgent-1_0.jar `
-r us-west-2 `
-glc environment-variable `
-gleo 'https://gamelift-alpha.us-west-2.amazonaws.com' `
-ip '205.251.233.50' `
-loc 'custom-location' `
-gslb '<user>-test-logs' `
-galb '<user>-test-logs' `
-galp 'C:\\GameLiftAgent\\Logs\\'
```
### Test GameLift Anywhere CLI Commands
1. CreateLocation
```
aws gamelift create-location \
  --region us-west-2 \
  --endpoint-url https://gl-proxy-alpha-tcp-pdx.pdx.proxy.amazon.com \
  --location-name custom-anywhere-fleet-location
```
1. CreateFleet
```
aws gamelift create-fleet \
 --region us-west-2 \
 --endpoint-url https://gl-proxy-alpha-tcp-pdx.pdx.proxy.amazon.com \
 --name test-anywhere-fleet \
 --compute-type ANYWHERE \
 --locations Location=custom-anywhere-fleet-location \
 --anywhere-configuration Cost=0.2 \
 --operating-system AMAZON_LINUX_2
```
1. RegisterCompute
```
aws gamelift register-compute \
 --region us-west-2 \
 --endpoint-url https://gl-proxy-alpha-tcp-pdx.pdx.proxy.amazon.com \
 --fleet-id fleet-5aef0063-1bb9-4db9-8f64-74a264566e71 \
 --compute-name test-anywhere-compute-1 \
 --ip-address 10.2.2.2 \
 --location custom-anywhere-fleet-location
```
1. DeregisterCompute
```
aws gamelift deregister-compute \
  --compute-name test-anywhere-compute-1 \
  --fleet-id fleet-5aef0063-1bb9-4db9-8f64-74a264566e71 \
  --endpoint-url https://gl-proxy-alpha-tcp-pdx.pdx.proxy.amazon.com
```

#### Test GameLiftAgent Logs

If an S3 bucket was provided for either game session logs or gamelift agent logs and the Fleet has a
valid `instanceRoleArn` then logs will be uploaded to the dev account S3 buckets specified.
If logs are not uploaded they should be located at `/local/game/logs` for game sessions
and at `/local/whitewater/Logs` for GameLiftAgent.

### Deploying

NOTE: This section is likely inaccurate for GameLiftAgent - it is an import from IPM (GLIFT-20131)

The GameLiftAgent is configured in `build.xml` to generate a standalone JAR when it builds, which packages the GameLiftAgent along with all the necessary dependencies into a single JAR that can be downloaded to the compute instance. When GameLiftAgent deploys to the pipeline, BATS is configured to upload this standalone JAR as an artifact to the BARS artifact S3 bucket through the configuration located under `configuration/aws_lambda/lambda-transform.yml`. When a region deploys, it will use the ID of the deployment to get the BARS S3 artifact and copy this standalone JAR to the destination S3 bucket in the WhiteWater service account.

#### Testing BATS

NOTE: This section is likely inaccurate for GameLiftAgent - it is an import from IPM (GLIFT-20131)

You can test the BATS configuration using the BATS CLI. After running the BATS CLI, run the following:

`bats transform -t GameLiftAgent-1.0 -p GameLiftAgent-1.0 -x AWSLambda-1.0 -o . -tc test-runtime -pc test-runtime`

This should produce a ZIP file that only contains the standalone JAR (as well as some empty directories potentially, but no other files)

### Logs

NOTE: This section is likely inaccurate for GameLiftAgent - it is an import from IPM (GLIFT-20131)

The application is configured to create a `logs` directory with a time-specific application log file (eg. GameLiftAgent.log.2021-10-16-00). The `logs` directory will be created in the location the launch command originated. Most of the time this will be the package root (same location as this README), but when launching the application from IntelliJ, the directory was created in the workspace root (two levels up from here).

The application log is configured to rotate every hour and be shared between the application and its subprocesses.

#### Extra Logging from ProcessBuilder
ProcessBuilder has a redirectError/Output which lets you redirect the I/O to a file. 

For Linux: 
If you want to use this, you will need to remove the 'setsid' within the command to get the proper I/O to the error file.
Setsid spins up the process within its own session (like a daemon). This ensures that if GameLiftAgent terminates, the process itself will not.

For Windows:
You will want to add -RedirectStandardError and/or -RedirectStandardOutput alongside a file name to output the logs.

### Check Git Commit Id From Jar
Run
```
unzip -q -c GameLiftAgent-1_0.jar META-INF/MANIFEST.MF
```

### Install Maven and Java 17 in your Environment
Note: GameLiftAgent was built with Java 17 and will require (at least) this version to compile. You can
check the current version of Java that your Maven installation will use to compile with the following command:
```
mvn -version
```
If it says mvn command not found, then install Maven with the instructions below.

For macOS run:
```
brew install maven
```
For DevDesktop run:
```
sudo yum install maven
```
If the version of maven shown is below 3.2.5, then you will have to update the version of Maven, or else you will see the error below.
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile (default-compile) on project GameLiftAgent: The plugin org.apache.maven.plugins:maven-compiler-plugin:3.11.0 requires Maven version 3.2.5 -> [Help 1]
```
Now check your java version.
```
java -version
```
If the java version is not showing Java 17, then you will have to install Java 17.

Here are the instructions for [DevDesktop](https://w.amazon.com/bin/view/Java_17/#HHowtoinstallJDK17onDevDesktop) and for [macOS](https://w.amazon.com/bin/view/Java_17/#HHowtoinstallJDK17onmacOS).

After you install Java 17 on your macOS, you may want to close your terminal and open a new one to open up a new Java 17 environment.
