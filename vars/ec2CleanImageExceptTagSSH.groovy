#!/usr/bin/env groovy
//
// ec2CleanImageExceptTagSSH.groovy
//
def call() {
    echo "Cleaning $APP_IMAGE_NAME except $APP_IMAGE_TAG on EC2 ... "

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'MY_INSTANCE_EC2_IP', variable: 'MY_INSTANCE_EC2_IP')
    ]) {
        sshagent(['EC2_SSH_KEY']) {
            sh '''
                set -euo pipefail
                echo "Cleaning ${DOCKER_USERNAME}/${APP_IMAGE_NAME} except ${APP_IMAGE_TAG} on EC2"

                # On Jenkins, prepare a Bash with Env Vars :
                ssh -o StrictHostKeyChecking=no ec2-user@$MY_INSTANCE_EC2_IP \
                    "DOCKER_USERNAME=$DOCKER_USERNAME \
                     APP_IMAGE_NAME=$APP_IMAGE_NAME \
                     APP_IMAGE_TAG=$APP_IMAGE_TAG \
                     bash -s" << 'SCRIPT'
# On EC2 :
set -euo pipefail
echo "Cleaning $DOCKER_USERNAME/$APP_IMAGE_NAME except $APP_IMAGE_TAG"

# Delete all <none> images
docker image prune -f

# List tag and ids of images, compare to image tag to keep then delete the others
docker images "$DOCKER_USERNAME/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" \
    | awk -v keep="$APP_IMAGE_TAG" '$1 != keep {print $2}'  | xargs -r docker rmi -f

SCRIPT
            '''
        }
    }
}
