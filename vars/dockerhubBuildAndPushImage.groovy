#!/usr/bin/env groovy
//
// dockerhubBuildAndPushImage.groovy
//
def call() {
    echo "Build and push image to Docker Hub..."

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'dockerhub-pat', variable: 'DOCKER_PAT')
    ]) {
        // Diagnoses 90% of jenkins+docker errors
        sh "whoami && id && docker --version && docker ps"

        sh '''
            set -euo pipefail   # stops if error (e), asks defined vars (u), checks all parts of pipeline (o pipefail)

            FULL_IMAGE="$DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

            echo "Building / Push Cleaning ${FULL_IMAGE} on Docker Hub"

            docker build --rm -t "$FULL_IMAGE" .   # clean the temporary files and add a tag to the image

            echo "$DOCKER_PAT" | docker login -u "$DOCKER_USERNAME" --password-stdin

            docker push "$FULL_IMAGE" --quiet

            docker logout
        '''
    }
}
