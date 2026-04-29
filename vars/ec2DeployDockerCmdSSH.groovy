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
            sh '''
                set -euo pipefail
                echo "Deploy ${DOCKER_USERNAME}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG} on EC2"

                # On Jenkins, prepare a Bash with Env Vars :
                ssh -o StrictHostKeyChecking=no ec2-user@$MY_INSTANCE_EC2_IP \
                    "DOCKER_USERNAME=$DOCKER_USERNAME \
                     APP_IMAGE_NAME=$APP_IMAGE_NAME \
                     APP_IMAGE_TAG=$APP_IMAGE_TAG \
                     APP_CONTAINER_NAME=$APP_CONTAINER_NAME \
                     APP_HOST_PORT=$APP_HOST_PORT \
                     APP_CONTAINER_PORT=$APP_CONTAINER_PORT \
                     bash -s" << 'SCRIPT'
# On EC2 :
set -euo pipefail
echo "Deploying $DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

# Pull the Image from Docker Hub
docker pull "$DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

# Replace the container with the last version of the App
docker stop "$APP_CONTAINER_NAME" || true
docker rm "$APP_CONTAINER_NAME" || true
docker run -d \
    --name "$APP_CONTAINER_NAME" \
    -p "$APP_HOST_PORT:$APP_CONTAINER_PORT" \
    "$DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"
SCRIPT
            '''
        }
    }
}
