#!/usr/bin/env groovy
//
// ecrBuildAndPushImageSSM.groovy
//
def call() {
    echo "Build and push image to ECR  ..."

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY')
    ]) {

        sh '''
            set -e

            FULL_IMAGE="$ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG"
            echo "... of $FULL_IMAGE"

            docker build --rm -t "$FULL_IMAGE" .
        
            # Verify IAM role
            set -x
            aws sts get-caller-identity

            # Create ECR repo if not exists
            {
                aws ecr describe-repositories --repository-names $APP_IMAGE_NAME --region eu-west-3 \
                || aws ecr create-repository --repository-name $APP_IMAGE_NAME --region eu-west-3
            } >/dev/null 2>&1

            # Login ECR via IAM role
            aws ecr get-login-password --region eu-west-3 \
                | docker login --username AWS --password-stdin $ECR_REGISTRY

            docker push "$FULL_IMAGE" --quiet

            docker logout $ECR_REGISTRY
        '''
    }
}
