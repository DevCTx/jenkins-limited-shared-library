#!/usr/bin/env groovy
//
// ec2CleanImageExceptTag.groovy
//
def call(String imageName, String imageTag) {
    echo "Cleaning all ${imageName} docker images except tag ${imageTag} on EC2..."

    def dockerCmd = """
        docker image prune -f
        docker images "${imageName}" --format "{{.Tag}} {{.ID}}" | grep -v "${imageTag}" | awk '{print \$2}' | xargs -r docker rmi -f
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
