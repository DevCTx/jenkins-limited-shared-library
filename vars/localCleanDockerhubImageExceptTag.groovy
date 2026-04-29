#!/usr/bin/env groovy
//
// localCleanDockerhubImageExceptTag.groovy
//
def call() {
    echo "Cleaning all dockerhub images except last tag on local..."

    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
    ]) {
        sh '''
            echo "... of $DOCKER_USERNAME/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG"

            # clean the images with no tag or <none>    
            docker image prune -f

            # List all tags and ids of the given image, except for the specified tag, and remove all others from these listed ids.        
            docker images "$DOCKER_USERNAME/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" \
            | grep -vw "^$APP_IMAGE_TAG " | awk '{print $2}' | xargs -r docker rmi -f
        '''
    }
}



