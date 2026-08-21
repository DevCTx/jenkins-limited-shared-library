#!/usr/bin/env groovy
//
// localCleanDockerhubImageExceptTag.groovy
//
def call() {
    withCredentials([
        string(credentialsId: 'dockerhub-username', variable: 'DOCKER_USERNAME')
    ]) {
        localCleanImageExceptTag(DOCKER_USERNAME)
    }
}