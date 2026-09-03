#!/usr/bin/env groovy
//
// localCleanImageExceptTag.groovy
//
def call(String repo_var_name) {
    echo "Cleaning all ${repo_var_name}/${APP_IMAGE_NAME} docker images except tag ${APP_IMAGE_TAG} on local..."

    // clean the images with no tag or <none>
    sh "docker image prune -f"

    // List all tags and ids of the given image, except for the specified tag, and remove all others from those listed ids.
    withEnv(["REPO_VAR_NAME=${repo_var_name}"]) {
        sh '''#!/bin/bash
            REPOSITORY="${!REPO_VAR_NAME}"
            docker images "$REPOSITORY/$APP_IMAGE_NAME" --format "{{.Tag}} {{.ID}}" \
            | awk -v tag="$APP_IMAGE_TAG" '$1 != tag {print $2}' \
            | xargs -r docker rmi -f
        '''
    }
}