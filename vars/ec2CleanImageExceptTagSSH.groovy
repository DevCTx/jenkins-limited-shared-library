#!/usr/bin/env groovy
//
// ec2CleanImageExceptTagSSH.groovy
//
def call() {
    echo "Cleaning $APP_IMAGE_NAME:$APP_IMAGE_TAG on EC2 ... "

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'MY_INSTANCE_EC2_IP', variable: 'MY_INSTANCE_EC2_IP')
    ]) {
        sshagent(['EC2_SSH_KEY']) {
            sh """
                ssh -o StrictHostKeyChecking=no ec2-user@${MY_INSTANCE_EC2_IP} '
                    set -euo pipefail

                    DOCKER_IMAGE="${DOCKER_USERNAME}/${APP_IMAGE_NAME}"
                    IMAGE_TAG="${APP_IMAGE_TAG}"

                    echo "Cleaning \$DOCKER_IMAGE except \$IMAGE_TAG"

                    docker image prune -f
                    docker images "\$DOCKER_IMAGE" --format "{{.Tag}} {{.ID}}" \
                    | awk -v keep="\$IMAGE_TAG" '$1 != keep {print $2}'  | xargs -r docker rmi -f

                '
            """
        }
    }
}
