#!/usr/bin/env groovy
//
// ec2DeployEcrDockerCmdSSH.groovy
//
def call() {
    echo "Deploying $APP_IMAGE_NAME:$APP_IMAGE_TAG on EC2 ... "

    withCredentials( [
        string(credentialsId: 'ecr-registry', variable: 'ECR_REGISTRY'),
        string(credentialsId: 'app-ec2-ip', variable: 'MY_INSTANCE_EC2_IP')
    ]) {
        sshagent(['app-ec2-key']) {
            sh '''
                set -euo pipefail
                echo "Deploy ${ECR_REGISTRY}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG} on EC2"

                # On Jenkins, prepare a Bash with Env Vars :
                ssh -o StrictHostKeyChecking=no ec2-user@$MY_INSTANCE_EC2_IP \
                    "ECR_REGISTRY=$ECR_REGISTRY \
                     APP_IMAGE_NAME=$APP_IMAGE_NAME \
                     APP_IMAGE_TAG=$APP_IMAGE_TAG \
                     APP_CONTAINER_NAME=$APP_CONTAINER_NAME \
                     APP_HOST_PORT=$APP_HOST_PORT \
                     APP_CONTAINER_PORT=$APP_CONTAINER_PORT \
                     bash -s" << 'SCRIPT'
# On EC2 :
set -euo pipefail
echo "Deploying $ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

# ECR requires a fresh login on the target instance (its own IAM role),
# unlike DockerHub which needs none to pull a public/already-authorized image.
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY

# Pull the Image from ECR
docker pull "$ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

# Replace the container with the last version of the App
docker stop "$APP_CONTAINER_NAME" || true
docker rm "$APP_CONTAINER_NAME" || true
docker run -d \
    --name "$APP_CONTAINER_NAME" \
    -p "$APP_HOST_PORT:$APP_CONTAINER_PORT" \
    "$ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG"
SCRIPT
            '''
        }
    }
}