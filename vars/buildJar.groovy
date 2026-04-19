#!/usr/bin/env groovy
//
// buildJar.groovy
//

def call() {
    echo "Building JAR file ..."
    sh "pwd"
    sh "ls -R"
    sh "find . -name pom.xml"    

    sh "mvn --no-transfer-progress -f pom.xml clean package"
}
