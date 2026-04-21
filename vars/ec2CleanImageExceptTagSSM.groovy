#!/usr/bin/env groovy
//
// ec2CleanImageExceptTagSSM.groovy
//
def call(String imageName, String imageTag) {
    echo "Cleaning all ${imageName} docker images except tag ${imageTag} on EC2 via AWS SSM..."

    withCredentials([
        string(credentialsId: 'PROD_EC2_ID', variable: 'PROD_EC2_ID')
    ]) {
        sh """
            aws ssm send-command \
              --document-name "AWS-RunShellScript" \
              --instance-ids ${PROD_EC2_ID} \
              --region eu-west-3 \
              --comment "Docker Compose with Env deployment" \
              --parameters commands='[
                "docker image prune -f",
                "docker images ${imageName} --format \\"{{.Tag}} {{.ID}}\\" | grep -v ${imageTag} | awk \\"{print \\$2}\\" | xargs -r docker rmi -f"
                ]'
        """ // SSM cmd can not be on multilines
    }
}
