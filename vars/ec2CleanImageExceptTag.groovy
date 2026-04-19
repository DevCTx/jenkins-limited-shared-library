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
                string(credentialsId: 'EC2_HOST', variable: 'EC2_HOST')
            ]) {
                sh 'ssh -o StrictHostKeyChecking=no ec2-user@$EC2_HOST "$DOCKER_CMD"'
            }
        }
    }
}
