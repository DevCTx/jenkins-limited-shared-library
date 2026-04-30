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

            # Verify IAM role only (hiding secret infos)
            aws sts get-caller-identity --output text --query 'Arn' | awk -F'/' '{print "Role: " $2}'

            # Build the cleaning script and encode it in base64 (one line, no return)
            # to avoid Groovy interpolation
            CLEAN_SCRIPT_B64=$(cat <<EOF |  base64 -w 0

# clean the images with no tag or <none>    
docker image prune -f

# List all tags and ids of the given image, except for the specified tag, and remove all others from these listed ids.        
docker images "$ECR_REGISTRY/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" \
    | awk -v keep="$APP_IMAGE_TAG" '\$1 != keep {print \$2}' | xargs -r docker rmi -f

EOF
)
            SSM_CMD="echo $CLEAN_SCRIPT_B64 | base64 -d | bash"

            CMD_ID=$(aws ssm send-command \
                --region eu-west-3 \
                --instance-ids $EC2_PROD_ID \
                --document-name "AWS-RunShellScript" \
                --comment "Cleaning ECR images on EC2 via SSM" \
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
