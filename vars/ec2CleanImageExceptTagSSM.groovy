#!/usr/bin/env groovy
//
// ec2CleanImageExceptTagSSM.groovy
//
def call() {
    echo "Cleaning ECR images on EC2 via SSM..."

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY'),
        string(credentialsId: 'EC2_PROD_ID', variable: 'EC2_PROD_ID'),
        string(credentialsId: 'S3_BUCKET', variable: 'S3_BUCKET')
    ]) {
        sh '''
            set -euo pipefail
            echo "Cleaning $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG on EC2"

            # Vérifier le rôle IAM
            aws sts get-caller-identity --output text --query Arn | awk -F/ '{print "Role: " $2}'

            echo "Prepare the JSON Command"
            cat > /tmp/ssm-clean.json <<EOF
{
    "InstanceIds": ["$EC2_PROD_ID"],
    "DocumentName": "AWS-RunShellScript",
    "Comment": "Cleaning ECR images on EC2",
    "Parameters": {
        "commands": [
            "docker image prune -f",
            "docker images ${ECR_REGISTRY}/${APP_IMAGE_NAME} --format '{{.Tag}} {{.ID}}' > /tmp/list-images.txt",
            "awk -v keep=${APP_IMAGE_TAG} '\$1 != keep {print \$2}' /tmp/list-images.txt | xargs -r docker rmi -f",
            "rm -f /tmp/list-images.txt"
        ]
    }
}
EOF

            echo "Send the JSON Command and clean on local"
            CMD_ID=$(aws ssm send-command \
                --region eu-west-3 \
                --cli-input-json file:///tmp/ssm-clean.json \
                --query 'Command.CommandId' \
                --output text)
            
            rm -f /tmp/ssm-clean.json

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
