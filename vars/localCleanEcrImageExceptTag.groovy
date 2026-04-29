#!/usr/bin/env groovy
//
// localCleanEcrImageExceptTag.groovy
//
def call() {
    echo "Cleaning ECR images on local..."

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY')
    ]) {
        sh '''
            set -euo pipefail
            echo "Cleaning $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG on local"

            # clean the images with no tag or <none>    
            docker image prune -f

            # List all tags and ids of the given image, except for the specified tag, and remove all others from these listed ids.        
            docker images "$ECR_REGISTRY/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" \
                | awk -v keep="$APP_IMAGE_TAG" '\$1 != keep {print \$2}' | xargs -r docker rmi -f
        '''
    }
}



