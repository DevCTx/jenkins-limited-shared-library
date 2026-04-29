#!/usr/bin/env groovy
//
// ecrBuildAndPushImageSSM.groovy
//
def call() {
    echo "Build and push image to ECR ..."

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY')
    ]) {
        sh '''
            set -euo pipefail

            FULL_IMAGE="$ECR_REGISTRY/$APP_IMAGE_NAME:$APP_IMAGE_TAG"

            echo "Building / Push Cleaning ${FULL_IMAGE} on ECR"

            docker build --rm -t "$FULL_IMAGE" .
        
            # Verify IAM role only (hiding secret infos)
            aws sts get-caller-identity --output text --query 'Arn' | awk -F'/' '{print "Role: " $2}'

            # Create ECR repo if not exists (hiding secret infos)
            if aws ecr describe-repositories --repository-names $APP_IMAGE_NAME --region eu-west-3 >/dev/null 2>&1; then
                echo "ECR repo $APP_IMAGE_NAME already exists"
            else
                echo "Creating ECR repo $APP_IMAGE_NAME ..."
                aws ecr create-repository --repository-name $APP_IMAGE_NAME --region eu-west-3 >/dev/null
                echo "ECR repo $APP_IMAGE_NAME created"
            fi

            # Login ECR via IAM role
            aws ecr get-login-password --region eu-west-3 \
                | docker login --username AWS --password-stdin $ECR_REGISTRY

            docker push "$FULL_IMAGE" --quiet

            docker logout $ECR_REGISTRY
        '''
    }
}

