#!/usr/bin/env groovy

def call(String containerName, String hostPort, String containerPort, String imageName, String imageTag) {
    echo "Deploying the application..."

    def dockerCmd = """
        docker pull ${imageName}:${imageTag} &&
        docker stop ${containerName} || true &&
        docker rm ${containerName} || true &&
        docker run -d --name ${containerName} -p ${hostPort}:${containerPort} ${imageName}:${imageTag}
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
