#!/usr/bin/env groovy
//
// ec2DockerComposeDeploy.groovy
//

def call(String containerName, String hostPort, String containerPort, String imageName, String imageTag) {
    echo "Deploying the application via Docker Compose on EC2 ... "

    def dockerCmd = """
        export CONTAINER=${containerName}
        export HOST_PORT=${hostPort}
        export CONTAINER_PORT=${containerPort}
        export IMAGE_NAME=${imageName}
        export IMAGE_TAG=${imageTag}

        docker-compose -f docker-compose.yaml down --remove-orphans --volumes
        docker-compose -f docker-compose.yaml up -d --quiet-pull
    """

    withEnv(["DOCKER_CMD=${dockerCmd}"]) {
        sshagent(['ec2-server-key']) {
            withCredentials([
                string(credentialsId: 'EC2_USER', variable: 'EC2_USER'),
                string(credentialsId: 'EC2_HOST', variable: 'EC2_HOST')
            ]) {
                sh 'scp docker-compose.yaml $EC2_USER@$EC2_HOST:/home/$EC2_USER/ '
                sh 'ssh -o StrictHostKeyChecking=no $EC2_USER@$EC2_HOST "$DOCKER_CMD"'
            }
        }
    }
}
