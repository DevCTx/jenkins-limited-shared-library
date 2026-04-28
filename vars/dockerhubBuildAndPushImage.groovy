#!/usr/bin/env groovy
//
// dockerhubBuildAndPushImage.groovy
//
def call(String imageName, String imageTag) {
    echo "docker build and push image to Docker Hub..."

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'dockerhub-pat', variable: 'DOCKER_PAT'),
        string(credentialsId: 'DOCKER_HUB_REPOSITORY', variable: 'DOCKER_HUB_REPOSITORY')
    ]) {
        withEnv(["IMG_TAG=${DOCKER_HUB_REPOSITORY}/${imageName}:${imageTag}"]) {
            // Diagnoses 90% of jenkins+docker errors
            echo "check docker env"
            sh "whoami && id && docker --version && docker ps"

            // --rm automatically removes intermediate containers
            sh '''
                docker build --rm -t "$IMG_TAG" .
                echo "$DOCKER_PAT" | docker login -u "$DOCKER_USERNAME" --password-stdin
                docker push "$IMG_TAG" --quiet
                docker logout
            '''
        }
    }
}
