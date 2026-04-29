#!/usr/bin/env groovy
//
// localCleanImageExceptTag.groovy
//
def call(String repository) {
    echo "Cleaning all ${repository}${APP_IMAGE_NAME} docker images except tag ${APP_IMAGE_TAG} on local..."

    // clean the images with no tag or <none>
    sh "docker image prune -f"

    // // List all tags and ids of the given image, except for the specified tag, and remove all others from those listed ids.

    def cmd = """
        docker image prune -f

        docker images "${repository}/${APP_IMAGE_NAME}" --format "{{.Tag}} {{.ID}}" \
        | awk '\$1 != "${APP_IMAGE_TAG}" {print \$2}' \
        | xargs -r docker rmi -f
    """
    
    echo cmd

    sh cmd
}



