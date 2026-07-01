# jenkins-limited-shared-library

A **project-scoped ("limited") Jenkins shared library** bundling custom pipeline steps for a full CI/CD flow: 
- building a **Maven JAR**, 
- building **Docker images**, 
- pushing them to **Docker Hub** or **AWS ECR**, 
- deploying to **AWS EC2** (via **SSH** or **SSM**), 
- and **cleaning up** old images.

> Used as a Git submodule of
> [PART3-Core / 9. Jenkins CI-CD](https://github.com/DevCTx/DevOps/tree/main/PART3-Core/9.Jenkins%20CI-CD/).



## What is a "limited" shared library?

Unlike a *global shared library* (available to every job on the controller), a
**project-scoped** library is declared at the level of a specific folder or job, so its reach is "limited" to that project. 

Each file in `vars/` becomes a **step** that can be called directly by name into a `Jenkinsfile`.


## Requirements

On the Jenkins agent: `mvn`, `docker`, the `aws` CLI (for the ECR/SSM steps), and SSH or SSM access to the target EC2 instances.

**Environment variables** expected by the steps:

| Variable | Purpose |
|---|---|
| `APP_IMAGE_NAME` | image / repository name |
| `APP_IMAGE_TAG` | tag to build / keep |
| `ECR_REGISTRY` | ECR registry URL (ECR/SSM steps) |

**Jenkins credentials** referenced:

| ID | Usage |
|---|---|
| `DOCKER_USERNAME` | Docker Hub username |
| `dockerhub-pat` | Docker Hub access token |
| *(AWS)* | credentials / IAM role for ECR, SSM and EC2 |

## Usage

Declare the library in the job or folder configuration (*Pipeline Libraries* section), then
call the steps by name:

```groovy
@Library('jenkins-limited-shared-library') _

pipeline {
    agent any
    environment {
        APP_IMAGE_NAME = 'my-app'
        APP_IMAGE_TAG  = "${env.BUILD_NUMBER}"
    }
    stages {
        stage('Build')  { steps { buildJar() } }
        stage('Image')  { steps { dockerhubBuildAndPushImage() } }
        stage('Deploy') { steps { ec2DeployDockerCmdSSH() } }
        stage('Clean')  { steps { dockerhubCleanImageExceptTag() } }
    }
}
```

## Step catalogue (`vars/`)

### Build
| Step | Purpose |
|---|---|
| `buildJar` | `mvn clean package -DskipTests` to produce the JAR |
| `localBuildJar` | same, for a local run |

### Docker Hub
| Step | Purpose |
|---|---|
| `dockerhubBuildAndPushImage` | builds the image and pushes it to Docker Hub |
| `dockerhubCleanImageExceptTag` | removes old tags via the registry API |
| `localCleanDockerhubImageExceptTag` | same cleanup, run locally |

### AWS ECR
| Step | Purpose |
|---|---|
| `ecrBuildAndPushImageSSM` | builds the image, creates the repo if needed, pushes to ECR |
| `ecrCleanImageExceptTagSSM` | cleans ECR images via SSM |
| `localCleanEcrImageExceptTag` | cleans ECR locally |

### AWS EC2 deployment
| Step | Purpose |
|---|---|
| `ec2DeployDockerCmdSSH` | deploys via SSH + `docker run` |
| `ec2DeployDockerComposeSSM` | deploys a `docker-compose` via SSM |
| `ec2DeployEnvDockerComposeSSM` | deploys compose + an environment file via SSM |
| `ec2CleanImageExceptTagSSH` | cleans images on the instance via SSH |
| `ec2CleanImageExceptTagSSM` | cleans images on the instance via SSM |

### Local cleanup
| Step | Purpose |
|---|---|
| `localCleanImageExceptTag(repository)` | removes local Docker images except the current tag and `<none>` ones |

## SSH or SSM?

Several steps come in two variants:

- **SSH** — direct connection to the instance; requires opening port 22 and managing a key.
- **SSM** (AWS Systems Manager) — runs commands without opening a port or distributing a key.
  This is the preferred option when Jenkins runs **inside AWS with an IAM role**:
  authentication goes through the role, with no credentials or ports to expose.

Pick the variant based on where your Jenkins controller lives.

## Skills demonstrated

Jenkins CI/CD (declarative pipeline, project-scoped shared library) · Groovy ·
Docker (build, push, cleanup) · Docker Hub & AWS ECR · AWS EC2 deployment over SSH and SSM ·
secure secret handling (`withCredentials`) · Maven.
