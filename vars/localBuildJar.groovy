#!/usr/bin/env groovy
//
// localBuildJar.groovy
//
def call() {
    echo "Building JAR file ..."
    sh "mvn --no-transfer-progress clean package -DskipTests"
}
