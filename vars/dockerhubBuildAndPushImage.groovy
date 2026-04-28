#!/usr/bin/env groovy
//
// dockerhubBuildAndPushImage.groovy
//
def call() {
    echo "docker build and push image to Docker Hub..."
    echo "${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'dockerhub-pat', variable: 'DOCKER_PAT')
    ]) {
        // Diagnoses 90% of jenkins+docker errors
        echo "check docker env"
        sh "whoami && id && docker --version && docker ps"

        sh '''
            IMAGE="$DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

            docker build --rm -t "$IMAGE" .

            echo "$DOCKER_PAT" | docker login -u "$DOCKER_USERNAME" --password-stdin

            docker push "$IMAGE" --quiet

            docker logout
        '''
    }
}
