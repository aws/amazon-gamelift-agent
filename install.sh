#
# Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
#

if type -p java; then
    echo found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo found java executable in JAVA_HOME     
    _java="$JAVA_HOME/bin/java"
else
    echo installing java 17
    curl -LJO https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz
    tar -xvzf amazon-corretto-17-x64-linux-jdk.tar.gz
    export JAVA_HOME=$(pwd)/amazon-corretto-17.0.10.8.1-linux-x64
    export PATH=$JAVA_HOME/bin:$PATH
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo version "$version"
    if [[ "$version" > "17" ]]; then
        echo version is greater than 17, meets requirement
    else         
        echo version is less than 17, needs to install java 17
        echo installing java 17
        curl -LJO https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz
        tar -xvzf amazon-corretto-17-x64-linux-jdk.tar.gz
        export JAVA_HOME=$(pwd)/amazon-corretto-17.0.10.8.1-linux-x64
        export PATH=$JAVA_HOME/bin:$PATH
    fi
fi

if type -p mvn; then
    echo found mvn executable in PATH
    _mvn=mvn
elif [[ -n "$MAVEN_HOME" ]] && [[ -x "$MAVEN_HOME/bin/mvn" ]];  then
    echo found mvn executable in MAVEN_HOME
    _mvn="$MAVEN_HOME/bin/mvn"
else
    echo installing maven 3.9.6
    curl -LJO https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip
    unzip apache-maven-3.9.6-bin.zip
    export MAVEN_HOME=$(pwd)/apache-maven-3.9.6
    export PATH=$MAVEN_HOME/bin:$PATH
fi

if [[ "$_mvn" ]]; then
    version=$("$_mvn" --version 2>&1 | awk -F'[ "]' '/Maven/ {print $3}')
    echo version "$version"
    if [[ "$version" > "3.2.5" ]]; then
        echo version is greater than 3.2.5, meets requirement
    else
        echo installing maven 3.9.6
        curl -LJO https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip
        unzip apache-maven-3.9.6-bin.zip
        export MAVEN_HOME=$(pwd)/apache-maven-3.9.6
        export PATH=$MAVEN_HOME/bin:$PATH
    fi
fi

export AGENT=https://github.com/[ACCOUNT]/[REPOSITORY]/archive/[BRANCH].zip
echo downloading Gamelift Agent from AGENT
curl -LJO AGENT
unzip [REPOSITORY]-[BRANCH].zip
cd [REPOSITORY]-[BRANCH]
mvn clean compile assembly:single