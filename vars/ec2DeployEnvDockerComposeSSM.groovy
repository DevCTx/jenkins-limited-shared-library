#!/usr/bin/env groovy
//
// ec2DeployEnvDockerComposeSSM.groovy
//
def call() {

    echo "Deploying application via AWS SSM..."

    ///////////////////////////////////////////////////////////////////////
    //
    //  SSM is really valuable when Jenkins is running on AWS with an IAM 
    //  role, and on not a server outside of AWS, because it will ask for 
    //  credentials to connect and some ports to open!
    //
    //  Requires creating Jenkins server on AWS to use SSM properly !!!
    //
    ///////////////////////////////////////////////////////////////////////

    // Encode the local files in base64 to facilitate the transfert 
    // but only with shell or it will be blocked with groovy policies
    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY')
    ]) {
        sh """
        cat > docker-compose.yaml <<'EOF'
services:
    ${APP_IMAGE_NAME}:
        image: ${ECR_REGISTRY}/${APP_IMAGE_NAME}:${APP_IMAGE_TAG}
        container_name: ${APP_CONTAINER_NAME}
        restart: unless-stopped
        ports:
            - "${APP_HOST_PORT}:${APP_CONTAINER_PORT}"
EOF
"""    
    }

    def docker_compose = sh(
        script: "base64 -w 0 docker-compose.yaml",
        returnStdout: true
    ).trim()

    withCredentials( [
        string(credentialsId: 'ECR_REGISTRY', variable: 'ECR_REGISTRY'),
        string(credentialsId: 'EC2_PROD_ID', variable: 'EC2_PROD_ID')
    ]) {
        // Verify IAM role
        sh 'aws sts get-caller-identity'        

        sh """
            aws ssm send-command \
                --region eu-west-3 \
                --instance-ids $EC2_PROD_ID \
                --document-name "AWS-RunShellScript" \
                --comment "Deploy docker-compose" \
                --parameters commands='[    
                      "sudo mkdir -p /opt/app",
                      "sudo chown -R ec2-user:docker /opt/app || true",
                      "echo $docker_compose | base64 -d > /opt/app/docker-compose.yaml",
                      "aws ecr get-login-password --region eu-west-3 | docker login --username AWS --password-stdin ${ECR_REGISTRY}",
                      "docker compose --project-directory /opt/app down || true",
                      "docker compose --project-directory /opt/app up -d"
                  ]' \
                --query 'Command.CommandId' \
                --output text > /tmp/ssm_deploy_cmd_id.txt         

            CMD_ID=\$(cat /tmp/ssm_deploy_cmd_id.txt)

            aws ssm wait command-executed \
                --region eu-west-3 \
                --instance-id $EC2_PROD_ID \
                --command-id \$CMD_ID
        """

        // Notes : 
        // cd /opt/app alone is useless in SSM
        // Each command in the SSM table runs in an independent shell 
        // so the cd does not persist for the next command.
        // use --project-directory /opt/app instead
        //
        // set +x   # disables the display of commands
   }
}
