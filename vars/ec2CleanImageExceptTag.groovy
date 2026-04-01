#!/usr/bin/env groovy

def call(String imageName, String imageTag) {
    echo "Cleaning all ${imageName} docker images except tag ${imageTag} on EC2..."

    def dockerCmd = """
        docker image prune -f
        docker images "${imageName}" --format "{{.Tag}} {{.ID}}" | grep -v "${imageTag}" | awk '{print \$2}' | xargs -r docker rmi -f
    """

    withEnv(["DOCKER_CMD=${dockerCmd}"]) {
        sshagent(['ec2-server-key']) {
            withCredentials([
                string(credentialsId: 'EC2_USER', variable: 'EC2_USER'),
                string(credentialsId: 'EC2_HOST', variable: 'EC2_HOST')
            ]) {
                sh 'ssh -o StrictHostKeyChecking=no $EC2_USER@$EC2_HOST "$DOCKER_CMD"'
            }
        }
    }
}
