#!/usr/bin/env groovy
//
// ec2CleanDockerhubImageExceptTagSSM.groovy
//
def call() {
    echo "Cleaning DockerHub images on EC2 via SSM..."

    withCredentials( [
        string(credentialsId: 'dockerhub-username', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'app-ec2-id', variable: 'EC2_PROD_ID')
    ]) {
        def cleanScript = '''
            set -euo pipefail
            echo "Cleaning $DOCKER_USERNAME/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG on EC2"

            # Vérifier le rôle IAM
            aws sts get-caller-identity --output text --query Arn | awk -F/ '{print "Role: " $2}'

            echo "Build the script to execute on EC2"
            REMOTE_SCRIPT=$(cat <<EOF

docker image prune -f

docker images $DOCKER_USERNAME/$APP_IMAGE_NAME --format '{{.Tag}} {{.ID}}' \\
    | grep -Fv '$APP_IMAGE_TAG ' | cut -d' ' -f2 | xargs -r docker rmi -f

EOF
)

            echo "# Prepare the JSON Command to send"
            JSON_PAYLOAD=$(jq -n \\
                --arg id     "$EC2_PROD_ID" \\
                --arg script "$REMOTE_SCRIPT" \\
                '{
                    InstanceIds:  [$id],
                    DocumentName: "AWS-RunShellScript",
                    Comment:      "Cleaning DockerHub images on EC2",
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
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                sh cleanScript
            }
        } catch (org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException e) {
            // no AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY set here, it means
            // Jenkins runs on AWS -> AWS CLI falls through its default
            // credential chain to the EC2 instance's IAM role
            sh cleanScript
        }
    }
}