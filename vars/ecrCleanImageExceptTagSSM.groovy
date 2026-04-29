#!/usr/bin/env groovy
//
// ecrCleanImageExceptTagSSM.groovy
//
def call() {
    echo "Cleaning ECR images via SSM..."


    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY')
    ]) {

        sh '''
            echo "... of $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG"

            # Verify IAM role
            aws sts get-caller-identity --query "{Account:Account, User:Arn}"

            # List all tags except the given tag
            IMAGES_TO_DELETE=$(aws ecr list-images \
                --region eu-west-3 \
                --repository-name "$ECR_REPOSITORY/$APP_IMAGE_NAME" \
                --query 'imageIds[?imageTag!="$APP_IMAGE_TAG"]' \
                --output json )

            # and delete them if the list is not empty
            if [ "$IMAGES_TO_DELETE" != "[]" ]; then
                aws ecr batch-delete-image \
                    --region eu-west-3 \
                    --repository-name "$ECR_REPOSITORY/$APP_IMAGE_NAME" \
                    --image-ids "$IMAGES_TO_DELETE"
            fi
        '''
    }
}
