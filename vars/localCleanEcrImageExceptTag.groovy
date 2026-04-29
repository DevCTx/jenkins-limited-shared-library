#!/usr/bin/env groovy
//
// localCleanEcrImageExceptTag.groovy
//
def call() {
    echo "Cleaning all ECR images except last tag on local..."

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY'),
    ]) {
        sh '''
            echo "... of $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG"

            # clean the images with no tag or <none>    
            docker image prune -f

            # List all tags and ids of the given image, except for the specified tag, and remove all others from these listed ids.        
            docker images "$ECR_REGISTRY/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" \
            | grep -vw "^$APP_IMAGE_TAG " | awk '{print $2}' | xargs -r docker rmi -f
        '''
    }
}



