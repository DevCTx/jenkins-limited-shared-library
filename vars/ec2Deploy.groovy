#!/usr/bin/env groovy
//
// ec2Deploy.groovy
//
def call(String containerName, String hostPort, String containerPort, String imageName, String imageTag) {
    echo "Deploying the application... "

    def dockerCmd = """
        docker pull ${imageName}:${imageTag} &&
        docker stop ${containerName} || true &&
        docker rm ${containerName} || true &&
        docker run -d --name ${containerName} -p ${hostPort}:${containerPort} ${imageName}:${imageTag}
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
