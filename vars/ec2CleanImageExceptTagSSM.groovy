#!/usr/bin/env groovy
//
// ec2CleanImageExceptTagSSM.groovy
//
def call() {
    echo "Cleaning EC2 images via AWS SSM..."

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY'),
        string(credentialsId: 'EC2_PROD_ID', variable: 'EC2_PROD_ID')
    ]) {
        echo "...of ${APP_IMAGE_NAME} except tag ${APP_IMAGE_TAG} "

        sh '''
            CMD_ID=\$(\
                aws ssm send-command \
                    --region eu-west-3 \
                    --instance-ids $EC2_PROD_ID \
                    --document-name "AWS-RunShellScript" \
                    --comment "Cleaning EC2 images via AWS SSM" \
                    --parameters commands='[
                        "docker image prune -f",
                        "docker images ${ECR_REGISTRY}/${APP_IMAGE_NAME} --format \"{{.Tag}} {{.ID}}\" | grep -v ${APP_IMAGE_TAG} | awk '{print \$2}' | xargs -r docker rmi -f"
                    ]' \
                    --query 'Command.CommandId' \
                    --output text
            )

            echo "SSM Command ID: \$CMD_ID"

            aws ssm wait command-executed \
                --region eu-west-3 \
                --instance-id $EC2_PROD_ID \
                --command-id \$CMD_ID
        '''
    }
}
