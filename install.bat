@echo off

java -version 1>nul 2>nul || (
   echo no java installed
   echo installing java 17
   curl -LJO https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.exe
   jdk-17_windows-x64_bin.exe /s
   setx JAVA_HOME=C:\Program Files\Java\jdk-17
)

for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "jver=%%j%%k"

if %jver% LSS 170 (
  echo %jver%
  echo java version is too low
  echo at least 17 is needed
  echo installing java 17
  curl -LJO https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.exe
  jdk-17_windows-x64_bin.exe /s
  setx JAVA_HOME=C:\Program Files\Java\jdk-17
)

mvn -version 1>nul 2>nul || (
  echo no maven installed
  echo installing maven 3.9.1
  set "maven=https://dlcdn.apache.org/maven/maven-3/3.9.1/binaries/apache-maven-3.9.1-bin.zip"
  echo downloading maven from %maven%
  curl -LJO %maven%
  set MAVEN_HOME=%cd%\apache-maven-3.9.1
  set PATH=%PATH%;%MAVEN_HOME%\bin
)

set "agent=https://github.com/[ACCOUNT]/[REPOSITORY]/archive/[BRANCH].zip"
echo downloading Gamelift Agent from %agent%
curl -LJO %agent%
tar -xf [REPOSITORY]-[BRANCH].zip

cd [REPOSITORY]-[BRANCH]
mvn clean compile assembly:single