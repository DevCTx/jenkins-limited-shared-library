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
            echo "... of $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG"

            # Verify IAM role
            aws sts get-caller-identity --query "{Account:Account, User:Arn}"

            CLEAN_CMD=$(cat <<'EOF'
docker image prune -f
docker images "$ECR_REGISTRY/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" 
| grep -vw "$APP_IMAGE_TAG" | awk '{print $2}' | xargs -r docker rmi -f
EOF
)

            CMD_ID=$(aws ssm send-command \
                --region eu-west-3 \
                --instance-ids "$EC2_PROD_ID" \
                --document-name "AWS-RunShellScript" \
                --comment "Cleaning ECR images on EC2 via SSM" \
                --parameters commands=["$CLEAN_CMD"] \
                --query 'Command.CommandId' \
                --output text)

            aws ssm wait command-executed \
                --region eu-west-3 \
                --instance-id "$EC2_PROD_ID" \
                --command-id "$CMD_ID"
        '''
    }
}
