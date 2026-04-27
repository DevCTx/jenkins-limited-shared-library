#!/usr/bin/env groovy
//
// ecrCleanImageExceptTag.groovy
//
def call(String imageName, String imageTag) {
    echo "Cleaning ECR images of ${imageName} except tag ${imageTag} via AWS SSM..."
     
    withEnv(["IMAGE_NAME=${imageName}", "IMAGE_TAG=${imageTag}"]) {
        sh '''
            # Short name of repo : "123.dkr.ecr…/demo-java-app" → "demo-java-app"
            # awk retrieves the number of fields between '/' and displays the last one.
            REPO_NAME=$(echo "$IMAGE_NAME" | awk -F'/' '{print $NF}')

            # List all tags except the given tag
            IMAGES_TO_DELETE=$(aws ecr list-images \
                --repository-name "$REPO_NAME" \
                --region eu-west-3 \
                --query "imageIds[?imageTag!='${IMAGE_TAG}']" \
                --output json)

            echo "Images to delete: $IMAGES_TO_DELETE"

            # and delete them if the list is not empty
            if [ "$IMAGES_TO_DELETE" != "[]" ]; then
                aws ecr batch-delete-image \
                    --repository-name "$REPO_NAME" \
                    --region eu-west-3 \
                    --image-ids "$IMAGES_TO_DELETE"
            fi
        '''
    }
}
