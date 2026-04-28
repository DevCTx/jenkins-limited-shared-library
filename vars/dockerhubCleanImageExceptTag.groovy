#!/usr/bin/env groovy
//
// dockerhubCleanImageExceptTag.groovy
//
def call(String imageName, String imageTag) {
    echo "Cleaning Docker Hub images of ${imageName} except tag ${imageTag} ..."
    
    withCredentials( [
        string(credentialsId: 'DOCKER_USERNAME', variable: 'DOCKER_USERNAME'),
        string(credentialsId: 'dockerhub-pat', variable: 'DOCKER_PAT'),
        string(credentialsId: 'DOCKER_HUB_REPOSITORY', variable: 'DOCKER_HUB_REPOSITORY')
    ]) {
        withEnv([
            "IMAGE_NAME=${DOCKER_HUB_REPOSITORY}/${imageName}",
            "IMAGE_TAG=${imageTag}"
        ]) {
            sh '''
                # List all tags except the given tag and delete the others
                curl -s "https://hub.docker.com/v2/repositories/$IMAGE_NAME/tags?page_size=100" \
                    | grep -o '"name":"[^"]*' | cut -d'"' -f4 \
                    | grep -v "^$IMAGE_TAG$" \
                    | while read tag; do
                        echo "Deleting tag: $tag"
                        set +x
                        curl -s -X DELETE "https://hub.docker.com/v2/repositories/$IMAGE_NAME/tags/$tag/" \
                            -H "Authorization: Bearer $DOCKER_PAT"
                        set -x
                    done
            '''
        }
    }
}
