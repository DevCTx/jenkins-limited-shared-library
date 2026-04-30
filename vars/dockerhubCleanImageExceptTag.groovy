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

            # Get a JSON Web Token(JWT) - PAT is not enough for DELETE but better than Password
            JWT=$(curl -s -X POST "https://hub.docker.com/v2/users/login" \
                -H "Content-Type: application/json" \
                -d "{\"username\": \"${DOCKER_USERNAME}\", \"password\": \"${DOCKER_PAT}\"}" \
                | grep -o '"token":"[^"]*' | cut -d'"' -f4 || true)

            if [ -z "$JWT" ]; then
                echo "Error: Failed to get JWT token from Docker Hub"
                exit 1
            fi

            # Lists the tag names of the current image, exclude the current tag, and delete all other tags of this image
            curl -s "https://hub.docker.com/v2/repositories/${DOCKER_USERNAME}/${APP_IMAGE_NAME}/tags?page_size=100" \
                | grep -o '"name":"[^"]*' | cut -d'"' -f4 \
                | grep -v "^${APP_IMAGE_TAG}" \
                | while read tag; do
                    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
                        "https://hub.docker.com/v2/repositories/${DOCKER_USERNAME}/${APP_IMAGE_NAME}/tags/$tag/" \
                        -H "Authorization: Bearer $JWT")
                    if echo "$STATUS" | grep -q "^2"; then
                        echo "Deleted tag: $tag (HTTP $STATUS)"
                    else
                        echo "Warning: failed to delete $tag (HTTP $STATUS)"
                    fi
                done || true
        '''

    }
}
