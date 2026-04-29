#!/usr/bin/env groovy
//
// ecrCleanImageExceptTagSSM.groovy
//
def call() {
    echo "Cleaning ECR images on ECR via SSM..."

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY')
    ]) {
        sh '''
            set -euo pipefail
            echo "Cleaning $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG on ECR"

            # Verify IAM role only (hiding secret infos)
            aws sts get-caller-identity --output text --query 'Arn' | awk -F'/' '{print "Role: " $2}'

            # List all tags except the given tag
            IMAGES_TO_DELETE=$(aws ecr list-images \
                --region eu-west-3 \
                --repository-name "$APP_IMAGE_NAME" \
                --query "imageIds[?imageTag!='$APP_IMAGE_TAG']" \
                --output json)

            # and delete them if the list is not empty
            if [ "$IMAGES_TO_DELETE" != "[]" ]; then
                aws ecr batch-delete-image \
                    --region eu-west-3 \
                    --repository-name "$APP_IMAGE_NAME" \
                    --image-ids "$IMAGES_TO_DELETE"
            fi
        '''
    }
}
