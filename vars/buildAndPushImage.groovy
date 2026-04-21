#!/usr/bin/env groovy
//
// buildAndPushImage.groovy
//

def call(String imageName, String imageTag) {
    echo "docker build and push image ..."

    withEnv(["IMG_TAG=${imageName}:${imageTag}"]) {
        withCredentials( [
            usernamePassword( credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKER_USERNAME',
				            passwordVariable: 'DOCKER_PASSWORD' )
        ]) {
            // Diagnoses 90% of jenkins+docker errors
            echo "check docker env"
            sh "whoami && id && docker --version && docker ps"

            // --rm automatically removes intermediate containers
            sh '''
                docker build --rm -t "$IMG_TAG" .
                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                docker push "$IMG_TAG" --quiet
                docker logout
            '''
        }
    }
}
