#!/usr/bin/env groovy
//
// dockerhubCleanImageExceptTag.groovy
//
def call() {
    echo "Cleaning $APP_IMAGE_NAME except $APP_IMAGE_TAG on Docker Hub ... "
    
    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'dockerhub-pat', variable: 'DOCKER_PAT')
    ]) {
        sh '''
            set -euo pipefail
            echo "Cleaning ${DOCKER_USERNAME}/${APP_IMAGE_NAME} except ${APP_IMAGE_TAG} on Docker Hub"

            curl -s "https://hub.docker.com/v2/repositories/${DOCKER_USERNAME}/${APP_IMAGE_NAME}/tags?page_size=100" \
                | grep -o '"name":"[^"]*' | cut -d'"' -f4 \
                | grep -v "^${APP_IMAGE_TAG}" \
                | while read tag; do
                    echo "Deleting tag: $tag"
                    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
                        "https://hub.docker.com/v2/repositories/${DOCKER_USERNAME}/${APP_IMAGE_NAME}/tags/$tag/" \
                        -H "Authorization: Bearer $DOCKER_PAT")
                    if echo "$STATUS" | grep -q "^2"; then
                        echo "Deleted tag: $tag (HTTP $STATUS)"
                    else
                        echo "Warning: failed to delete $tag (HTTP $STATUS)"
                    fi
                done
        '''
    }
}

