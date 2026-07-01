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
            set -euo pipefail   # stops if error (e), asks defined vars (u), checks all parts of pipeline (o pipefail)

            echo "Cleaning ${DOCKER_USERNAME}/${APP_IMAGE_NAME} except ${APP_IMAGE_TAG} on Docker Hub"

            set +x  # Undisplays the commands in the log (hide the JWT)

            # Get a JSON Web Token(JWT) - PAT is not enough for DELETE but better than Password
            echo "Get JWT from Docker Hub"
            # - sets a JSON object with credentials
            # - sends the login request without progression (-s)
            # - asks to read a file (@) from standard input (-) into the data (d) specifying a JSON header (H)
            # - response:{"token":"eyJhbGciOiJ..."} so isolates the 4th field : empty|token|:|eyJhbGciOiJ...|empty
            JWT=$(printf '{"username":"%s","password":"%s"}' "${DOCKER_USERNAME}" "${DOCKER_PAT}" \
                    | curl -s -X POST "https://hub.docker.com/v2/users/login" \
                        -H "Content-Type: application/json" -d @- \
                    | grep -o '"token":"[^"]*' | cut -d'"' -f4 || true)

            echo "Check JWT"
            if [ -z "$JWT" ]; then
                echo "Error: Failed to get JWT token from Docker Hub"
                exit 1
            fi

            # Lists the tag names of the current image, exclude the current tag, and delete all other tags of this image
            # - lists 100 first tags of the repo
            # - isolate name and keep 4th field
            # - keep all except tag (-v = reverse)
            # - for each remaining tag, send an authenticated DELETE request and capture only the HTTP status code
            # - if the status starts with 2 (2xx = success), log the deletion, otherwise log a warning
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

