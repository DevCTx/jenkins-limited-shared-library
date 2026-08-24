#!/usr/bin/env groovy
//
// ecrCleanImageExceptTag.groovy
//
def call() {
    echo "Cleaning ECR images on ECR..."

    withCredentials( [
        string(credentialsId: 'ecr-registry', variable: 'ECR_REGISTRY')
    ]) {
        def cleanScript = '''
            set -euo pipefail   # stops if error (e), asks defined vars (u), checks all parts of pipeline (o pipefail)

            echo "Cleaning $ECR_REGISTRY/$APP_IMAGE_NAME except tag $APP_IMAGE_TAG on ECR"

            # Verify IAM role only (hiding secret infos)
            aws sts get-caller-identity --output text --query 'Arn' | awk -F'/' '{print "Role: " $2}'

            # List all tags except the given tag
            IMAGES_TO_DELETE=$(aws ecr list-images \\
                --repository-name "$APP_IMAGE_NAME" \\
                --query "imageIds[?imageTag!='$APP_IMAGE_TAG']" \\
                --output json)

            # and delete them if the list is not empty
            if [ "$IMAGES_TO_DELETE" != "[]" ]; then
                aws ecr batch-delete-image \\
                    --repository-name "$APP_IMAGE_NAME" \\
                    --image-ids "$IMAGES_TO_DELETE"
            fi
        '''

        try {
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                sh cleanScript
            }
        } catch (org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException e) {
            // no AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY set here, it means
            // Jenkins runs on AWS -> AWS CLI falls through its default
            // credential chain to the EC2 instance's IAM role
            sh cleanScript
        }
    }
}