#!/usr/bin/env groovy
//
// ec2DeployDockerhubCmdSSM.groovy
//
def call() {
    echo "Deploying $APP_IMAGE_NAME:$APP_IMAGE_TAG to EC2 via SSM..."

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
        string(credentialsId: 'dockerhub-username', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'app-ec2-id', variable: 'EC2_PROD_ID')
    ]) {
        sh '''
            set -euo pipefail

            echo "Deploying $DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG to EC2"

            # Vérifier le rôle IAM
            aws sts get-caller-identity --output text --query Arn | awk -F/ '{print "Role: " $2}'

            echo "Build the script to execute on EC2"
            REMOTE_SCRIPT=$(cat <<EOF

# DockerHub needs no login here, unlike ECR: a public/already-authorized
# image can be pulled directly.
docker pull $DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG

docker stop $APP_CONTAINER_NAME || true
docker rm $APP_CONTAINER_NAME || true

docker run -d \
    --name $APP_CONTAINER_NAME \
    -p $APP_HOST_PORT:$APP_CONTAINER_PORT \
    $DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG

EOF
)

            echo "# Prepare the JSON Command to send"
            JSON_PAYLOAD=$(jq -n \
                --arg id     "$EC2_PROD_ID" \
                --arg script "$REMOTE_SCRIPT" \
                '{
                    InstanceIds:  [$id],
                    DocumentName: "AWS-RunShellScript",
                    Comment:      "Deploy container",
                    Parameters:   { commands: [$script] }
                }')

            echo "Send the JSON Command and clean on local"
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