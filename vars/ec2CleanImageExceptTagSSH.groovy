#!/usr/bin/env groovy
//
// ec2CleanImageExceptTagSSH.groovy
//
def call() {
    echo "Cleaning all dockerhub images except last tag on EC2..."

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
    ]) {
        def dockerCmd = '''
            echo "... of $DOCKER_USERNAME/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG"

            docker image prune -f
            docker images "$DOCKER_USERNAME/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" \
            | grep -vw "$APP_IMAGE_TAG" | awk '{print $2}' | xargs -r docker rmi -f
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
