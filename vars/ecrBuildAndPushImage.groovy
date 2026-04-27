#!/usr/bin/env groovy
//
// ecrBuildAndPushImage.groovy
//
def call(String imageName, String imageTag) {
    echo "docker build and push image to ECR  ..."

    withEnv(["IMG_TAG=${ECR_REPOSITORY}/${imageName}:${imageTag}"]) {
        sh '''
            # Verify IAM role
            aws sts get-caller-identity

            # Create ECR repo if not exists
            REPO_NAME=$(echo "$IMG_TAG" | awk -F'/' '{print $NF}' | cut -d: -f1)
            aws ecr describe-repositories --repository-names "$REPO_NAME" --region eu-west-3 \
                || aws ecr create-repository --repository-name "$REPO_NAME" --region eu-west-3

            # Login ECR via IAM role
            aws ecr get-login-password --region eu-west-3 \
                | docker login --username AWS --password-stdin $ECR_REPOSITORY

            docker build --rm -t "$IMG_TAG" .
            docker push "$IMG_TAG" --quiet
            docker logout $ECR_REPOSITORY
        '''
    }
}
