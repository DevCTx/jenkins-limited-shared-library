#!/usr/bin/env groovy
//
// ec2DeployDockerCmdSSH.groovy
//
def call() {
    echo "Deploying $APP_IMAGE_NAME:$APP_IMAGE_TAG on EC2 ... "

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'MY_INSTANCE_EC2_IP', variable: 'MY_INSTANCE_EC2_IP')
    ]) {
        sshagent(['EC2_SSH_KEY']) {
            sh """
                ssh -o StrictHostKeyChecking=no ec2-user@${MY_INSTANCE_EC2_IP} '
                    set -e

                    DOCKER_IMAGE="${DOCKER_USERNAME}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"
                    CONTAINER_NAME="${APP_CONTAINER_NAME}"
                    HOST_PORT="${APP_HOST_PORT}"
                    CONTAINER_PORT="${APP_CONTAINER_PORT}"

                    echo "Deploying \$DOCKER_IMAGE"

                    docker pull "\$DOCKER_IMAGE"

                    docker stop "\$CONTAINER_NAME" || true
                    docker rm "\$CONTAINER_NAME" || true

                    docker run -d \
                        --name "\$CONTAINER_NAME" \
                        -p "\$HOST_PORT:\$CONTAINER_PORT" \
                        "\$DOCKER_IMAGE"
                '
            """
        }
    }
}
