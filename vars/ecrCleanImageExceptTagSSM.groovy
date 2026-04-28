#!/usr/bin/env groovy
//
// ecrCleanImageExceptTagSSM.groovy
//
def call() {
    echo "Cleaning ECR images via AWS SSM..."
     
    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY')
    ]) {
        echo "... of ${ECR_REGISTRY}/${APP_IMAGE_NAME} except tag ${APP_IMAGE_TAG} "

        sh """
            # Verify IAM role
            aws sts get-caller-identity

            # List all tags except the given tag
            IMAGES_TO_DELETE=$(aws ecr list-images \
                --repository-name ${ECR_REPOSITORY}/${APP_IMAGE_NAME} \
                --region eu-west-3 \
                --query 'imageIds[?imageTag!="${APP_IMAGE_TAG}"]' \
                --output json)

            echo "Images to delete: $IMAGES_TO_DELETE"

            # and delete them if the list is not empty
            if [ "$IMAGES_TO_DELETE" != "[]" ]; then
                aws ecr batch-delete-image \
                    --repository-name ${ECR_REPOSITORY}/${APP_IMAGE_NAME} \
                    --region eu-west-3 \
                    --image-ids "$IMAGES_TO_DELETE"
            fi
        """
    }
}
