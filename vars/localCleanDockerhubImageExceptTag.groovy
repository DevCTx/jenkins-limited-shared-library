#!/usr/bin/env groovy
//
// localCleanDockerhubImageExceptTag.groovy
//
def call() {
    echo "Cleaning $APP_IMAGE_NAME:$APP_IMAGE_TAG on local ... "
    
    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME')
    ]) {
        sh """
            DOCKER_IMAGE="${DOCKER_USERNAME}/${APP_IMAGE_NAME}"
            IMAGE_TAG="${APP_IMAGE_TAG}"

            echo "Cleaning \$DOCKER_IMAGE except \$IMAGE_TAG"

            # clean the images with no tag or <none>    
            docker image prune -f

            # List all tags and ids of the given image, except for the specified tag, and remove all others from these listed ids.        
            docker images \$DOCKER_IMAGE --format "{{.Tag}} {{.ID}}" \
            | awk -v keep="\$IMAGE_TAG" '$1 != keep {print $2}' | xargs -r docker rmi -f
        '''
    }
}


