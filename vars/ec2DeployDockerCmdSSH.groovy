#!/usr/bin/env groovy
//
// ec2DeployDockerCmdSSH.groovy
//
def call() {
    echo "Deploying the application... "

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
    ]) {
        def dockerCmd = '''
            FULL_IMAGE="$DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG"
            echo "... of $FULL_IMAGE"

            docker pull "$FULL_IMAGE" &&
            docker stop "$APP_CONTAINER_NAME" || true &&
            docker rm "$APP_CONTAINER_NAME" || true &&
            docker run -d --name "$APP_CONTAINER_NAME" -p "$APP_HOST_PORT:$APP_CONTAINER_PORT" "$FULL_IMAGE"
        '''

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
}
