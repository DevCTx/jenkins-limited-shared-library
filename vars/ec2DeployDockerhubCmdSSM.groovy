#!/usr/bin/env groovy
//
// ec2DeployDockerhubCmdSSM.groovy
//
def call() {
    echo "Deploying $APP_IMAGE_NAME:$APP_IMAGE_TAG to EC2 via SSM..."

    withCredentials([
        string(credentialsId: 'dockerhub-username', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'app-ec2-id', variable: 'EC2_PROD_ID')
    ]) {
        def deployScript = '''
            set -euo pipefail

            echo "Deploying $DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG to EC2"

            # Vérifier le rôle IAM
            aws sts get-caller-identity --output text --query Arn | awk -F/ '{print "Role: " $2}'

            echo "Build the script to execute on EC2"
            REMOTE_SCRIPT=$(cat <<EOF

docker pull $DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG

docker stop $APP_CONTAINER_NAME || true
docker rm $APP_CONTAINER_NAME || true

docker run -d \\
    --name $APP_CONTAINER_NAME \\
    -p $APP_HOST_PORT:$APP_CONTAINER_PORT \\
    $DOCKER_USERNAME/$APP_IMAGE_NAME:$APP_IMAGE_TAG

EOF
)

            echo "# Prepare the JSON Command to send"
            JSON_PAYLOAD=$(jq -n \\
                --arg id     "$EC2_PROD_ID" \\
                --arg script "$REMOTE_SCRIPT" \\
                '{
                    InstanceIds:  [$id],
                    DocumentName: "AWS-RunShellScript",
                    Comment:      "Deploy container",
                    Parameters:   { commands: [$script] }
                }')

            echo "Send the JSON Command and clean on local"
            CMD_ID=$(aws ssm send-command \\
                --cli-input-json "$JSON_PAYLOAD" \\
                --query 'Command.CommandId' --output text)

            aws ssm wait command-executed \\
                --instance-id "$EC2_PROD_ID" \\
                --command-id "$CMD_ID"

            aws ssm get-command-invocation \\
                --instance-id "$EC2_PROD_ID" \\
                --command-id "$CMD_ID" \\
                --query '{Status:Status, Output:StandardOutputContent, Error:StandardErrorContent}' \\
                --output json
        '''

        try {
            // AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are injected here (Jenkins runs locally)
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                sh deployScript   // "aws ..." finds these 2 vars -> authenticates with them
            }
        } catch (org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException e) {
            // Here, neither AWS_ACCESS_KEY_ID nor AWS_SECRET_ACCESS_KEY is set (Jenkins runs on AWS)
            // no AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY set here, it means
            // Jenkins runs on AWS -> AWS CLI falls through its default
            // credential chain to the EC2 instance's IAM role
            sh deployScript
        }
    }
}