#!/usr/bin/env groovy
//
// ec2CleanImageExceptTagSSM.groovy
//
def call() {
    echo "Cleaning ECR images on EC2 via SSM..."

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY'),
        string(credentialsId: 'EC2_PROD_ID', variable: 'EC2_PROD_ID')
    ]) {
        sh '''
            set -euo pipefail
            echo "Cleaning $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG on EC2"

            # Vérifier le rôle IAM
            aws sts get-caller-identity --output text --query Arn | awk -F/ '{print "Role: " $2}'

            echo "Build the script to execute on EC2"
            REMOTE_SCRIPT=$(cat <<EOF

docker image prune -f

docker images $ECR_REGISTRY/$APP_IMAGE_NAME --format '{{.Tag}} {{.ID}}' \
    | grep -Fv '$APP_IMAGE_TAG ' | cut -d' ' -f2 | xargs -r docker rmi -f

EOF
)

            echo "# Prepare the JSON Command to send"
            JSON_PAYLOAD=$(jq -n \
                --arg id     "$EC2_PROD_ID" \
                --arg script "$REMOTE_SCRIPT" \
                '{
                    InstanceIds:  [$id],
                    DocumentName: "AWS-RunShellScript",
                    Comment:      "Cleaning ECR images on EC2",
                    Parameters:   { commands: [$script] }
                }')

            echo "Send the JSON Command and clean on local"
            CMD_ID=$(echo "$PAYLOAD" \
                | aws ssm send-command \
                    --cli-input-json file:///dev/stdin \
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
