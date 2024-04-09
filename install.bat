: ' Copyright Amazon.com Inc. or its affiliates. All Rights Reserved. '

@echo off

java -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
   echo No Java installed
   echo Installing Amazon Corretto 17
   curl -LJO https://corretto.aws/downloads/latest/amazon-corretto-17-x64-windows-jdk.zip
   powershell -Command "Expand-Archive -Path amazon-corretto-17-x64-windows-jdk.zip -DestinationPath 'C:\Program Files\Amazon Corretto'"
   set "JAVA_HOME=C:\Program Files\Amazon Corretto\jdk17.0.10_7"
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JVER=%%g
)
set JVER=%JVER:"=%

IF "%JVER%" LSS "17.0" (
    echo Java version is too low (%JVER%)
    echo At least 17 is needed
    echo Installing Amazon Corretto 17
    curl -LJO https://corretto.aws/downloads/latest/amazon-corretto-17-x64-windows-jdk.zip
    powershell -Command "Expand-Archive -Path amazon-corretto-17-x64-windows-jdk.zip -DestinationPath 'C:\Program Files\Amazon Corretto'"
    set "JAVA_HOME=C:\Program Files\Amazon Corretto\jdk17.0.10_7"
)

mvn -version 1>nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
   echo No Maven installed
   echo Installing Apache Maven 3.9.6
   curl -LJO https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip
   powershell -Command "Expand-Archive -Path apache-maven-3.9.6-bin.zip -DestinationPath 'C:\Program Files\Apache Maven'"
   set "MAVEN_HOME=C:\Program Files\Apache Maven\apache-maven-3.9.6"
   setx /M PATH "%PATH%;%MAVEN_HOME%\bin"
) ELSE (
   for /f "tokens=3" %%g in ('mvn -version 2^>^&1 ^| findstr /i "version"') do (
       set MVER=%%g
   )
   set MVER=%MVER:"=%

   IF "%MVER%" LSS "3.2.5" (
       echo Maven version is too low (%MVER%)
       echo Installing Apache Maven 3.9.6
       curl -LJO https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip
       powershell -Command "Expand-Archive -Path apache-maven-3.9.6-bin.zip -DestinationPath 'C:\Program Files\Apache Maven'"
       set "MAVEN_HOME=C:\Program Files\Apache Maven\apache-maven-3.9.6"
       setx /M PATH "%PATH%;%MAVEN_HOME%\bin"
   )
)

set "agent=https://github.com/[ACCOUNT]/[REPOSITORY]/archive/[BRANCH].zip"
echo downloading Gamelift Agent from %agent%
curl -LJO %agent%
tar -xf [REPOSITORY]-[BRANCH].zip

cd [REPOSITORY]-[BRANCH]
mvn clean compile assembly:single