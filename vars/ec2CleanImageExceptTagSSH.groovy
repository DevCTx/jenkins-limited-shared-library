#!/usr/bin/env groovy
//
// ec2CleanImageExceptTagSSH.groovy
//
def call() {
    echo "Cleaning all ${APP_IMAGE_NAME} docker images except tag ${APP_IMAGE_TAG} on EC2..."

    def dockerCmd = """
        docker image prune -f
        docker images "${APP_IMAGE_NAME}" --format "{{.Tag}} {{.ID}}" \
        | grep -v "${APP_IMAGE_TAG}" | awk '{print \$2}' | xargs -r docker rmi -f
    """

    withEnv(["DOCKER_CMD=${dockerCmd}"]) {
        sshagent(['EC2_SSH_KEY']) {
            withCredentials([
                string(credentialsId: 'MY_INSTANCE_EC2_IP', variable: 'MY_INSTANCE_EC2_IP')
            ]) {
                sh 'ssh -o StrictHostKeyChecking=no ec2-user@$MY_INSTANCE_EC2_IP "$DOCKER_CMD"'
            }
        }
    }
}
