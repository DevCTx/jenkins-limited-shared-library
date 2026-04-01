#!/usr/bin/env groovy

def dockerhubCleanImageExceptTag(String imageName, String imageTag) {
    echo "Cleaning all ${imageName} docker images except tag ${imageTag} on Docker Hub..."

    withCredentials([usernamePassword(
        credentialsId: 'dockerhub-credentials',
        usernameVariable: 'DOCKER_USERNAME',
        passwordVariable: 'DOCKER_PASSWORD'
    )]) {
        sh """
            # 1. Retrieve the JWT token
            TOKEN=\$(curl -s -X POST "https://hub.docker.com/v2/users/login" \
                -H "Content-Type: application/json" \
                -d '{"username": "'\$DOCKER_USERNAME'", "password": "'\$DOCKER_PASSWORD'"}' \
                | grep -o '"token":"[^"]*' | cut -d'"' -f4)

            # 2. List all tags except the given tag and delete the others
            curl -s "https://hub.docker.com/v2/repositories/${imageName}/tags?page_size=100" \
                | grep -o '"name":"[^"]*' | cut -d'"' -f4 \
                | grep -v "^${imageTag}\$" \
                | while read tag; do
                    echo "Deleting tag: \$tag"
                    curl -s -X DELETE "https://hub.docker.com/v2/repositories/${imageName}/tags/\$tag/" \
                        -H "Authorization: Bearer \$TOKEN"
                done
        """
    }
}
