#!/usr/bin/env groovy
//
// ec2DeployDockerCmdSSH.groovy
//
def call() {
    echo "Deploying the application... "

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'MY_INSTANCE_EC2_IP', variable: 'MY_INSTANCE_EC2_IP')
    ]) {
        sshagent(['EC2_SSH_KEY']) {
            sh '''
                ssh -o StrictHostKeyChecking=no ec2-user@$MY_INSTANCE_EC2_IP '
                    set -e

                    echo "Deploying $DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

                    docker pull "$DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

                    docker stop "$APP_CONTAINER_NAME" || true
                    docker rm "$APP_CONTAINER_NAME" || true

                    docker run -d \
                        --name "$APP_CONTAINER_NAME" \
                        -p "$APP_HOST_PORT:$APP_CONTAINER_PORT" \
                        "$DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"
                '
            '''
        }
    }
}
