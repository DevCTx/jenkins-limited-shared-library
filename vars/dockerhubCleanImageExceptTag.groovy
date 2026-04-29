#!/usr/bin/env groovy
//
// dockerhubCleanImageExceptTag.groovy
//
def call() {
    echo "Cleaning $APP_IMAGE_NAME:$APP_IMAGE_TAG on Docker Hub ... "
    
    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'dockerhub-pat', variable: 'DOCKER_PAT')
    ]) {
        sh """
            DOCKER_IMAGE="${DOCKER_USERNAME}/${APP_IMAGE_NAME}"
            IMAGE_TAG="${APP_IMAGE_TAG}"
            TOKEN="${DOCKER_PAT}"

            echo "Cleaning \$DOCKER_IMAGE except \$IMAGE_TAG"

            # List all tags except the given tag and delete the others
            curl -s "https://hub.docker.com/v2/repositories/\$DOCKER_IMAGE/tags?page_size=100" \
                | grep -o '"name":"[^"]*' | cut -d'"' -f4 \
                | grep -v "^\$IMAGE_TAG" \
                | while read tag; do
                    echo "Deleting tag: $tag"
                    set +x
                    curl -s -X DELETE "https://hub.docker.com/v2/repositories/\$DOCKER_IMAGE/tags/$tag/" \
                        -H "Authorization: Bearer \$TOKEN"
                    set -x
                done
        """
    }
}

