#!/usr/bin/env groovy
//
// localCleanDockerhubImageExceptTag.groovy
//
def call() {
    echo "Cleaning $APP_IMAGE_NAME except $APP_IMAGE_TAG on local ... "
    
    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME')
    ]) {
        sh '''
            set -euo pipefail
            echo "Cleaning ${DOCKER_USERNAME}/${APP_IMAGE_NAME} except ${APP_IMAGE_TAG} on local"

            # clean the images with no tag or <none>    
            docker image prune -f

            # List all tags and ids of the given image, except for the specified tag, and remove all others from these listed ids.        
            docker images ${DOCKER_USERNAME}/${APP_IMAGE_NAME} --format "{{.Tag}} {{.ID}}" \
                | awk -v keep="${APP_IMAGE_TAG}" '\$1 != keep {print \$2}' | xargs -r docker rmi -f
        '''
    }
}

