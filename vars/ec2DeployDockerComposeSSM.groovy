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

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY'),
        string(credentialsId: 'EC2_PROD_ID', variable: 'EC2_PROD_ID')
    ]) {
        sh '''
            set -euo pipefail
            echo "Build docker-commpose.yaml"

            # Build docker-compose and encode it in base64 (one line, no return)
            # to avoid Groovy interpolation
            DOCKER_COMPOSE_B64=$(cat <<EOF | base64 -w 0
services:
    $APP_IMAGE_NAME:
        image: $ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG
        container_name: $APP_CONTAINER_NAME
        restart: unless-stopped
        ports:
            - "$APP_HOST_PORT:$APP_CONTAINER_PORT"
EOF
            )

            # Build the deployment script and encode it in base64 (one line, no return)
            # to avoid Groovy interpolation
            DEPLOY_SCRIPT_B64=$(cat <<EOF  | base64 -w 0
set -euo pipefail
echo "Prepare docker-commpose.yaml and run it on EC2 after log into ECR"

sudo mkdir -p /opt/app
sudo chown -R ec2-user:docker /opt/app || true

echo "$DOCKER_COMPOSE_B64" | base64 -d > /opt/app/docker-compose.yaml

aws ecr get-login-password --region eu-west-3 \
    | docker login --username AWS --password-stdin $ECR_REGISTRY

docker compose --project-directory /opt/app down --remove-orphans || true
docker compose --project-directory /opt/app up -d

# Notes : "cd /opt/app" alone is useless in SSM
# Each command in the SSM table runs in an independent shell 
# so the cd does not persist for the next command.
# use --project-directory /opt/app instead

EOF
            )

            # Verify IAM role only (hiding secret infos)
            aws sts get-caller-identity | jq -r '"Role: " + (.Arn | split("/")[1])'

            SSM_CMD="echo $DEPLOY_SCRIPT_B64 | base64 -d | bash"

            CMD_ID=$(aws ssm send-command \
                --region eu-west-3 \
                --instance-ids $EC2_PROD_ID \
                --document-name "AWS-RunShellScript" \
                --comment "Deploy docker-compose" \
                --parameters "commands=[\"$SSM_CMD\"]" \
                --query 'Command.CommandId' \
                --output text)

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
