#!/usr/bin/env groovy

def call(String imageName, String version) {
    echo "docker build and push image ..."

    withEnv(["IMG_VER=${imageName}:${version}"]) {
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
                docker build --rm -t "$IMG_VER" .
                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                docker push "$IMG_VER"
                docker logout
            '''
        }
    }
}
