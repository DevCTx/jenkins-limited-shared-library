#!/usr/bin/env groovy
//
// ec2DeployDockerComposeSSM.groovy
//
def call() {
    echo "Deploying Docker Compose to EC2 via SSM..."

    ///////////////////////////////////////////////////////////////////////
    //
    //  SSM is really valuable when Jenkins is running on AWS with an IAM 
    //  role, and on not a server outside of AWS, because it will ask for 
    //  credentials to connect and some ports to open!
    //
    //  Requires creating Jenkins server on AWS to use SSM properly !!!
    //
    ///////////////////////////////////////////////////////////////////////

    withCredentials([
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY'),
        string(credentialsId: 'EC2_PROD_ID', variable: 'EC2_PROD_ID'),
        string(credentialsId: 'S3_BUCKET', variable: 'S3_BUCKET')
    ]) {
        sh '''
            set -euo pipefail
            echo "Deploying $ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG to EC2"

            # Vérifier le rôle IAM
            aws sts get-caller-identity --output text --query Arn | awk -F/ '{print "Role: " $2}'

            echo "Build the docker-compose.yaml"
            cat > /tmp/docker-compose.yaml <<EOF
services:
    $APP_IMAGE_NAME:
        image: $ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG
        container_name: $APP_CONTAINER_NAME
        restart: unless-stopped
        ports:
            - "$APP_HOST_PORT:$APP_CONTAINER_PORT"
EOF

            echo "Upload to S3 and clean on local"
            aws s3 cp /tmp/docker-compose.yaml s3://$S3_BUCKET/docker-compose.yaml
            rm -f /tmp/docker-compose.yaml

            echo "# Prepare the JSON Command to send"
            cat > /tmp/ssm-input.json <<EOF
{
    "InstanceIds": ["$EC2_PROD_ID"],
    "DocumentName": "AWS-RunShellScript",
    "Comment": "Deploy docker-compose",
    "Parameters": {
        "commands": [
            "sudo mkdir -p /opt/app",
            "aws s3 cp s3://$S3_BUCKET/docker-compose.yaml /opt/app/docker-compose.yaml",
            "aws ecr get-login-password --region eu-west-3 | docker login --username AWS --password-stdin $ECR_REGISTRY >/dev/null",
            "docker compose --project-directory /opt/app down --remove-orphans || true",
            "docker compose --project-directory /opt/app up -d"
        ]
    }
}
EOF

            echo "Send the JSON Command and clean on local"
            # Déployer via SSM
            CMD_ID=$(aws ssm send-command \
                --region eu-west-3 \
                --cli-input-json file:///tmp/ssm-input.json \
                --query 'Command.CommandId' \
                --output text)

            rm -f /tmp/ssm-input.json

            aws ssm wait command-executed \
                --region eu-west-3 \
                --instance-id $EC2_PROD_ID \
                --command-id $CMD_ID

            aws ssm get-command-invocation \
                --region eu-west-3 \
                --instance-id $EC2_PROD_ID \
                --command-id $CMD_ID \
                --query '{Status: Status, Output: StandardOutputContent, Error: StandardErrorContent}' \
                --output json
        '''
    }
}
