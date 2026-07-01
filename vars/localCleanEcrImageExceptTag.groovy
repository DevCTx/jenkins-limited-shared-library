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
            set -euo pipefail   # stops if error (e), asks defined vars (u), checks all parts of pipeline (o pipefail)

            echo "Cleaning $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG on local"

            # clean the images with no tag or <none>    
            docker image prune -f

            # List all tags and ids of the given image, except for the specified tag, and remove all others from these listed ids. 
            # - list all tags ids like : 1.0 a1b2c3d4, 0.9 e5f6g7h8, latest a1b2c3d4 ...
            # - print all the tags which are not like 'keep'
            # - inject them to the docker rmi forcing the remove or do nothing if empty (-r)       
            docker images "$ECR_REGISTRY/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" \
                | awk -v keep="$APP_IMAGE_TAG" '\$1 != keep {print \$2}' | xargs -r docker rmi -f
        '''
    }
}



