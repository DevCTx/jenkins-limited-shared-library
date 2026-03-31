#!/usr/bin/env groovy

def call(String imageName, String version) {
    echo "withCredentials"
    withCredentials( [
        usernamePassword( credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
				        passwordVariable: 'DOCKER_PASSWORD' )
    ]) {
        // Diagnoses 90% of jenkins+docker errors
        echo "check docker env"
        sh "whoami && id && docker --version && docker ps"

	    echo "docker build image ..."       // --rm automatically removes intermediate containers
        sh '''
            docker build --rm -t '${imageName}:${version}' . 
        '''
        
	    echo "docker login and push image ..."
        sh '''
            echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
            docker push '${imageName}:${version}'
            docker logout
        '''
    }
}
