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
    def dot_env = sh(
        script: "base64 -w 0 .env",
        returnStdout: true
    ).trim()

    def docker_compose = sh(
        script: "base64 -w 0 docker-compose.yaml",
        returnStdout: true
    ).trim()

    withEnv([
        "DOT_ENV=${dot_env}", 
        "DOCKER_COMPOSE=${docker_compose}"
    ]) {        
        withCredentials([
            string(credentialsId: 'PROD_EC2_ID', variable: 'PROD_EC2_ID')
        ]) {

            sh 'aws sts get-caller-identity'
            

            def commandId = sh(
                script: '''
                    set +x
                    aws ssm send-command \
                      --document-name "AWS-RunShellScript" \
                      --instance-ids $PROD_EC2_ID \
                      --region eu-west-3 \
                      --comment "Docker Compose with Env deployment" \
                      --parameters commands='[
                            "sudo mkdir -p /opt/app",
                            "sudo chown -R ec2-user:docker /opt/app || true",
                            "echo '"$DOT_ENV"' | base64 -d > /opt/app/.env",
                            "echo '"$DOCKER_COMPOSE"' | base64 -d > /opt/app/docker-compose.yaml",
                            "cd /opt/app && docker compose down --remove-orphans || true",
                            "cd /opt/app && docker compose up -d --quiet-pull"
                        ]' \
                      --query 'Command.CommandId' \
                      --output text
                ''',
                returnStdout: true
            ).trim()
            // Notes :
            // cd /opt/app alone is useless in SSM
            // Each command in the SSM table runs in an independent shell 
            // so the cd does not persist for the next command.
            //
            // set +x   # disables the display of commands


            echo "SSM Command ID: ${commandId}"

            // Wait for the execution (pull image) to finish
            sh """
                aws ssm wait command-executed \
                  --command-id ${commandId} \
                  --instance-id \$PROD_EC2_ID \
                  --region eu-west-3
            """
            
            // Check the status
            def status = sh(
                script: """
                    aws ssm get-command-invocation \
                      --command-id ${commandId} \
                      --instance-id \$PROD_EC2_ID \
                      --region eu-west-3 \
                      --query 'Status' \
                      --output text
                """,
                returnStdout: true
            ).trim()

            echo "SSM Command Status: ${status}"

            if (status != 'Success') {
                // Display logs in case of failure
                sh """
                    aws ssm get-command-invocation \
                      --command-id ${commandId} \
                      --instance-id \$PROD_EC2_ID \
                      --region eu-west-3 \
                      --query 'StandardErrorContent' \
                      --output text
                """
                error("SSM deployment failed with status: ${status}")
            }
        }
    }
}
