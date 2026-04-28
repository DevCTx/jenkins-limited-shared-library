#!/usr/bin/env groovy
//
// ecrBuildAndPushImage.groovy
//
def call() {
    echo "docker build and push image to ECR  ..."

    withCredentials( [
        string(credentialsId: 'ECR_REGITRY', variable: 'ECR_REGITRY')
    ]) {
        echo "Build of ${ECR_REGITRY}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"
        sh "docker build --rm -t ${ECR_REGITRY}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG} ."

        echo "Push of ${ECR_REGITRY}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"
        sh """
            # Verify IAM role
            aws sts get-caller-identity

            # Create ECR repo if not exists
            aws ecr describe-repositories --repository-names ${ECR_REGITRY}/${APP_IMAGE_NAME} --region eu-west-3 \
                || aws ecr create-repository --repository-name ${ECR_REGITRY}/${APP_IMAGE_NAME} --region eu-west-3

            # Login ECR via IAM role
            aws ecr get-login-password --region eu-west-3 \
                | docker login --username AWS --password-stdin ${ECR_REGITRY}

            docker push ${ECR_REGITRY}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG} --quiet
            docker logout $ECR_REPOSITORY/${APP_IMAGE_NAME}
        """
    }
}
