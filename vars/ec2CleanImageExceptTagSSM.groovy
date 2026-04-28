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
        echo "...of ${ECR_REGISTRY}/${APP_IMAGE_NAME} except tag ${APP_IMAGE_TAG} "

        sh """
            aws ssm send-command \
              --document-name "AWS-RunShellScript" \
              --instance-ids ${EC2_PROD_ID} \
              --region eu-west-3 \
              --comment "Docker Compose with Env deployment" \
              --parameters '{
                    "commands":["docker image prune -f",
                    "docker images ${ECR_REGISTRY}/${APP_IMAGE_NAME} --format \\"{{.Tag}} {{.ID}}\\" \
                    | grep -v ${APP_IMAGE_TAG} | awk \\"{print \$2}\\" | xargs -r docker rmi -f"]
                }'
        """
    }
}
