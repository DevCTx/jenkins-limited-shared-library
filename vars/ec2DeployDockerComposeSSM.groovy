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
        string(credentialsId: 'EC2_PROD_ID', variable: 'EC2_PROD_ID')
    ]) {
        sh '''
            set -euo pipefail

            echo "Deploying $ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG to EC2"

            # Vérifier le rôle IAM
            aws sts get-caller-identity --output text --query Arn | awk -F/ '{print "Role: " $2}'

            echo "Build the docker-compose.yaml"
            DOCKER_COMPOSE=$(cat <<EOF
services:

  $APP_IMAGE_NAME:
    image: $ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG
    container_name: $APP_CONTAINER_NAME
    restart: unless-stopped
    ports:
      - "$APP_HOST_PORT:$APP_CONTAINER_PORT"

EOF
)

            echo "Build the script to execute on EC2"
            REMOTE_SCRIPT=$(cat <<EOF
mkdir -p /opt/app

cat > /opt/app/docker-compose.yaml <<'YAML'
$DOCKER_COMPOSE
YAML

aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY >/dev/null

docker compose --project-directory /opt/app down --remove-orphans || true
docker compose --project-directory /opt/app up -d

EOF
)

            echo "# Prepare the JSON Command to send"
            JSON_PAYLOAD=$(jq -n \
                --arg id     "$EC2_PROD_ID" \
                --arg script "$REMOTE_SCRIPT" \
                '{
                    InstanceIds:  [$id],
                    DocumentName: "AWS-RunShellScript",
                    Comment:      "Deploy docker-compose",
                    Parameters:   { commands: [$script] }
                }')

            echo "Send the JSON Command and clean on local"
            # Déployer via SSM
            CMD_ID=$(aws ssm send-command \
                --cli-input-json "$JSON_PAYLOAD" \
                --query 'Command.CommandId' --output text)

            aws ssm wait command-executed \
                --instance-id "$EC2_PROD_ID" \
                --command-id "$CMD_ID"

            aws ssm get-command-invocation \
                --instance-id "$EC2_PROD_ID" \
                --command-id "$CMD_ID" \
                --query '{Status:Status, Output:StandardOutputContent, Error:StandardErrorContent}' \
                --output json
        '''
    }
}
