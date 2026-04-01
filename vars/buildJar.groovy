#!/usr/bin/env groovy

def buildJar() {
    echo "Building JAR file ..."
    sh "mvn --no-transfer-progress clean package"
}
