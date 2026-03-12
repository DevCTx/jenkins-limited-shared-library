#!/usr/bin/env groovy

def call() {
    echo "withCredentials"
    withCredentials( [
        usernamePassword( credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
				        passwordVariable: 'DOCKER_PASSWORD' )
    ]) {
        // Diagnoses 90% of jenkins+docker errors
        echo "check docker env"
        sh "whoami && id && docker --version && docker ps"

	    echo "docker build image ..."
        sh "docker build -t ${params.IMAGE_NAME}:${params.VERSION} ."
        
	    echo "docker login and push image ..."
        sh "echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin"
        sh "docker push ${params.IMAGE_NAME}:${params.VERSION}"
    }
}
