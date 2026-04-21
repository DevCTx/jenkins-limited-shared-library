#!/usr/bin/env groovy
//
// ec2DockerComposeDeployEnvSSM.groovy
//
def call() {

    echo "Deploying application via AWS SSM..."

    withCredentials([
        string(credentialsId: 'PROD_EC2_ID', variable: 'PROD_EC2_ID')
    ]) {

        def bucket = "bucket/app/${env.BUILD_NUMBER}"

        sh """
        set -e

        aws s3 cp docker-compose.yaml s3://${bucket}/
        aws s3 cp .env s3://${bucket}/

        aws ssm send-command \
          --document-name "AWS-RunShellScript" \
          --instance-ids ${PROD_EC2_ID} \
          --region eu-west-3 \
          --comment "Docker Compose with Env deployment" \
          --parameters commands='[
            "set -e",

            "export BUCKET=${bucket}",

            "sudo mkdir -p /opt/app",
            "sudo chown -R ec2-user:docker /opt/app || true",

            "aws s3 cp s3://\\$BUCKET/.env /opt/app/.env",
            "aws s3 cp s3://\\$BUCKET/docker-compose.yaml /opt/app/docker-compose.yaml",

            "cd /opt/app",
            "docker compose down --remove-orphans || true",
            "docker compose up -d --quiet-pull"
          ]'
        """
    }
}
