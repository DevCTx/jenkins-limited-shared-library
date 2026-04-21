#!/usr/bin/env groovy
//
// dockerhubCleanImageExceptTag.groovy
//
def call(String imageName, String imageTag) {
    echo "Cleaning all ${imageName} docker images except tag ${imageTag} on Docker Hub..."
    
    withEnv(["IMAGE_NAME=${imageName}", "IMAGE_TAG=${imageTag}"]) {
        withCredentials([usernamePassword(
            credentialsId: 'dockerhub-credentials',
            usernameVariable: 'DOCKER_USERNAME',
            passwordVariable: 'DOCKER_PASSWORD'
        )]) {
            sh '''
                # Hide the Token
                set +x

                # Get the Json Web Token (JWT)
                TOKEN=$(curl -s -X POST "https://hub.docker.com/v2/users/login" \
                    -H "Content-Type: application/json" \
                    -d '{"username": "'"$DOCKER_USERNAME"'", "password": "'"$DOCKER_PASSWORD"'"}' \
                    | grep -o '"token":"[^"]*' | cut -d'"' -f4)
                set -x

                # List all tags except the given tag and delete the others
                curl -s "https://hub.docker.com/v2/repositories/$IMAGE_NAME/tags?page_size=100" \
                    | grep -o '"name":"[^"]*' | cut -d'"' -f4 \
                    | grep -v "^$IMAGE_TAG$" \
                    | while read tag; do
                        echo "Deleting tag: $tag"
                        set +x
                        curl -s -X DELETE "https://hub.docker.com/v2/repositories/$IMAGE_NAME/tags/$tag/" \
                            -H "Authorization: Bearer $TOKEN"
                        set -x
                    done
            '''
        }
    }
}
