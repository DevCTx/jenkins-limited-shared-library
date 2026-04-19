#!/usr/bin/env groovy
//
// buildJar.groovy
//

def call() {
    echo "Building JAR file ..."
    sh "mvn --no-transfer-progress -f pom.xml clean package"
}
