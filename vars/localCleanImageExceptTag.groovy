#!/usr/bin/env groovy

def call(String imageName, String imageTag) {
    echo "Cleaning all ${imageName} docker images except tag ${imageTag} on local..."

    withEnv(["IMG_NAME=${imageName}", "IMG_TAG=${imageTag}"]) {

        // clean the images with no tag or <none>
        sh 'docker image prune -f'        

        // // List all tags and ids of the given image, except for the specified tag, and remove all others from those listed ids.
        sh """
            docker images "$IMG_NAME" --format "{{.Tag}} {{.ID}}" | grep -v "$IMG_TAG" | awk '{print \$2}' | xargs -r docker rmi -f '
        """
    }
}



