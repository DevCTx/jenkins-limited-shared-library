#!/usr/bin/env groovy
//
// ec2DockerComposeDeployEnvSSM.groovy
//
def call() {

    echo "Deploying application via AWS SSM..."

    withCredentials([
        string(credentialsId: 'PROD_EC2_ID', variable: 'PROD_EC2_ID')
    ]) {

        // Encode the local files in base64 to facilitate the transfert 
        // but only with shell or it will be blocked with groovy policies
        def env = sh(
            script: "base64 -w 0 .env",
            returnStdout: true
        ).trim()

        def compose = sh(
            script: "base64 -w 0 docker-compose.yaml",
            returnStdout: true
        ).trim()

        sh """
        aws ssm send-command \
          --document-name "AWS-RunShellScript" \
          --instance-ids $PROD_EC2_ID \
          --comment "Docker Compose with Env deployment" \
          --parameters commands='[
            "sudo mkdir -p /opt/app",
            "sudo chown -R ec2-user:docker /opt/app || true",

            "echo ${env} | base64 -d > /opt/app/.env",
            "echo ${compose} | base64 -d > /opt/app/docker-compose.yaml",

            "cd /opt/app",
            "docker compose down --remove-orphans",
            "docker compose up -d --quiet-pull"
          ]'
        """
    }
}
