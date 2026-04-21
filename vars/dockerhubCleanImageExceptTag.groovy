#!/usr/bin/env groovy
//
// dockerhubCleanImageExceptTag.groovy
//
def call(String imageName, String imageTag) {
    echo "Cleaning all ${imageName} docker images except tag ${imageTag} on Docker Hub..."
    
    withCredentials([usernamePassword(
        credentialsId: 'dockerhub-credentials',
        usernameVariable: 'DOCKER_USERNAME',
        passwordVariable: 'DOCKER_PASSWORD'
    )]) {
        withEnv(["IMAGE_NAME=${imageName}", "IMAGE_TAG=${imageTag}"]) {
            sh '''
                # Retrieve the Json Web Token (JWT)
                # TOKEN=$(curl -s -X POST "https://hub.docker.com/v2/users/login" \
                #    -H "Content-Type: application/json" \
                #    -d '{"username": "'"$DOCKER_USERNAME"'", "password": "'"$DOCKER_PASSWORD'"}' \
                #    | grep -o '"token":"[^"]*' | cut -d'"' -f4)

                # List all tags except the given tag and delete the others
                curl -s "https://hub.docker.com/v2/repositories/$IMAGE_NAME/tags?page_size=100" \
                    | grep -o '"name":"[^"]*' | cut -d'"' -f4 \
                    | grep -v "^$IMAGE_TAG$" \
                    | while read tag; do
                        echo "Deleting tag: $tag"
                        curl -s -X DELETE "https://hub.docker.com/v2/repositories/$IMAGE_NAME/tags/$tag/" \
                             -H "Authorization: Bearer $DOCKER_PASSWORD"
                 #           -H "Authorization: Bearer $TOKEN"
                    done
            '''
        }
    }
}
