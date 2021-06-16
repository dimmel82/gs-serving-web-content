def globalDynamicVars=[:]

pipeline {
  agent {
    label "jenkins-jenkins-agent"
  }
//  options {
//    timestamps()
//  }
//  parameters {
//    choice(name: 'MINUTES_WAIT_ON_FAILURE', choices: ['2', '0', '10', '30'], description: 'The number of minutes to wait before exiting the builder pod when the pipeline fails')
//  }
  environment {
    AWS_CREDENTIALS = credentials('aws')
    AWS_ACCESS_KEY_ID = "${env.AWS_CREDENTIALS_USR}"
    AWS_SECRET_ACCESS_KEY = "${env.AWS_CREDENTIALS_PSW}"
    SONARQUBE_PROJECT_NAME = "${env.APP_NAME}-${env.ENVIRONMENT_ID}"
    SONARQUBE_SERVER_ID = 'sonarqube'
  }
//  tools {
//    dockerTool 'docker' 
//  }
  stages {
    stage('Build') {
      steps {
        dir('complete') {
          script {
            def pom=readMavenPom file: 'pom.xml'
            
            globalDynamicVars.appName=pom.artifactId
            globalDynamicVars.appVersion=pom.version
            
            globalDynamicVars.imageTag="${globalDynamicVars.appVersion}-${env.BRANCH}-${env.BUILD_NUMBER}"
            globalDynamicVars.imageName=globalDynamicVars.appName.replaceAll('\\.', '-')
            globalDynamicVars.chartVersion=globalDynamicVars.imageTag.toLowerCase()
            globalDynamicVars.imageRepo=globalDynamicVars.imageName
            globalDynamicVars.registryImageRepo="${DOCKER_REGISTRY_HOST}/${globalDynamicVars.imageRepo}"
            globalDynamicVars.image="${globalDynamicVars.registryImageRepo}:${globalDynamicVars.imageTag}"
          }
          
          // Install Helm
          sh 'curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 && chmod 700 get_helm.sh && export HELM_INSTALL_DIR=${WORKSPACE}/helm_bin && ./get_helm.sh'
          
          // Prepare and validate Helm chart
          sh "sed -i \"s@^version:.*@version: ${globalDynamicVars.chartVersion}@g\" helmChart/Chart.yaml"
          sh "sed -i \"s@repository:.*@repository: ${args.registryImageRepo}@g\" helmChart/values.yaml"
          sh "sed -i \"s@tag:.*@tag: ${args.imageTag}@g\" helmChart/values.yaml"
          sh "${WORKSPACE}/helm_bin/helm lint --values dev_values.yaml helmChart"
          
          sh 'chmod u+x mvnw'
          
          // Install aws cli
          sh 'curl https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o awscliv2.zip && unzip awscliv2.zip && ./aws/install --install-dir ${WORKSPACE}/aws_cli --bin-dir ${WORKSPACE}/aws_cli_bin'
          
          configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
            script {
              def ecrPassword=sh script: '${WORKSPACE}/aws_cli_bin/aws ecr get-login-password', returnStdout: true
              sh script: "${WORKSPACE}/aws_cli_bin/aws ecr create-repository --repository-name ${globalDynamicVars.imageRepo}", returnStatus: true
              
              withEnv(["DOCKER_PWD=${ecrPassword}"]) {
                sh "./mvnw -s $MAVEN_SETTINGS clean package dockerfile:push -Ddocker.image.repo=${globalDynamicVars.registryImageRepo} -Ddocker.image.tag=${globalDynamicVars.imageTag} -Ddockerfile.username=AWS"
              }
            }
          }
        }
      }
    }
    stage('Deploy Dev') {
      when { branch 'develop' }
      steps {
        dir('complete/helmChart') {
          sh "${WORKSPACE}/helm_bin/helm upgrade ${globalDynamicVars.appName} --install --namespace dev-apps --create-namespace --atomic --values dev_values.yaml ."
        }
      }
  }
  post {
		always {
//      sleep 10000
      echo 'test'
/*		script {
				slackSend(
						color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
						channel: '#sagan-content',
						message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\n${env.BUILD_URL}")
      }
*/
		}
  }
}
