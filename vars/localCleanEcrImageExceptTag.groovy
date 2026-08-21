#!/usr/bin/env groovy
//
// localCleanEcrImageExceptTag.groovy
//
def call() {
    withCredentials([
        string(credentialsId: 'ecr-registry', variable: 'ECR_REGISTRY')
    ]) {
        localCleanImageExceptTag(ECR_REGISTRY)
    }
}