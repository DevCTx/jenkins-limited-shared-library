#!/usr/bin/env groovy

def call() {
    echo "Building JAR file ..."
    sh "mvn --no-transfer-progress clean package"
}
