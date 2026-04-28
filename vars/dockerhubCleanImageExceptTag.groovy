#!/usr/bin/env groovy
//
// dockerhubCleanImageExceptTag.groovy
//
def call() {
    echo "Cleaning Docker Hub images of ${APP_IMAGE_NAME} except tag ${APP_IMAGE_TAG} ..."
    
    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'dockerhub-pat', variable: 'DOCKER_PAT')
    ]) {
        sh """
            # List all tags except the given tag and delete the others
            curl -s "https://hub.docker.com/v2/repositories/${DOCKER_USERNAME}/${APP_IMAGE_NAME}/tags?page_size=100" \
                | grep -o '"name":"[^"]*' | cut -d'"' -f4 \
                | grep -v "^${APP_IMAGE_TAG}" \
                | while read tag; do
                    echo "Deleting tag: \$tag"
                    set +x
                    curl -s -X DELETE "https://hub.docker.com/v2/repositories/${DOCKER_USERNAME}/${APP_IMAGE_NAME}/tags/\$tag/" \
                        -H "Authorization: Bearer ${DOCKER_PAT}"
                    set -x
                done
        """
    }
}
