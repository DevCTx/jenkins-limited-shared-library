#!/usr/bin/env groovy
//
// ec2DeployDockerCmdSSH.groovy
//
def call(String containerName, String hostPort, String containerPort, String repoName, String imageName, String imageTag) {
    echo "Deploying the application... "

    withEnv(["IMG_TAG=${repoName}/${imageName}:${imageTag}"]) {
        def dockerCmd = """
            # docker pull ${imageName}:${imageTag} &&
            docker pull ${IMG_TAG} &&
            docker stop ${containerName} || true &&
            docker rm ${containerName} || true &&
            docker run -d --name ${containerName} -p ${hostPort}:${containerPort} ${IMG_TAG}
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
}
