#!/usr/bin/env groovy
//
// dockerhubBuildAndPushImageSSH.groovy
//
def call() {
    echo "docker build and push image to Docker Hub..."
    echo "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'dockerhub-pat', variable: 'DOCKER_PAT')
    ]) {
        withEnv(["IMG_TAG=${DOCKER_USERNAME}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"]) 
        {
            // Diagnoses 90% of jenkins+docker errors
            echo "check docker env"
            sh "whoami && id && docker --version && docker ps"

            echo "Build ${IMG_TAG}"
            sh """
                docker build --rm -t ${IMG_TAG} .
            """
            // --rm automatically removes intermediate containers

            echo "Push ${IMG_TAG}"
            sh """
                echo ${DOCKER_PAT} | docker login -u ${DOCKER_USERNAME} --password-stdin
                docker push ${IMG_TAG} --quiet
                docker logout
            """
        }
    }
}
