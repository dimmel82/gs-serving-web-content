def globalDynamicVars=[:]

pipeline {
  agent {
    label "jenkins-jenkins-agent"
  }
  options {
    timestamps()
  }
//  parameters {
//    choice(name: 'MINUTES_WAIT_ON_FAILURE', choices: ['2', '0', '10', '30'], description: 'The number of minutes to wait before exiting the builder pod when the pipeline fails')
//  }
  environment {
    AWS_CREDENTIALS = credentials('aws')
    AWS_ACCESS_KEY_ID = "${env.AWS_CREDENTIALS_USR}"
    AWS_SECRET_ACCESS_KEY = "${env.AWS_CREDENTIALS_PSW}"
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
            
            globalDynamicVars.imageTag="${globalDynamicVars.appVersion}-${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
            globalDynamicVars.imageName=globalDynamicVars.appName.replaceAll('\\.', '-')
            globalDynamicVars.chartVersion=globalDynamicVars.imageTag.toLowerCase()
            globalDynamicVars.imageRepo=globalDynamicVars.imageName
            globalDynamicVars.registryImageRepo="${DOCKER_REGISTRY_HOST}/${globalDynamicVars.imageRepo}"
            globalDynamicVars.image="${globalDynamicVars.registryImageRepo}:${globalDynamicVars.imageTag}"
          }
          
          // Install Helm
          sh '''curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 && \
                chmod 700 get_helm.sh && \
                export USE_SUDO=false && \
                export HELM_INSTALL_DIR=${WORKSPACE}/helm_bin && \
                export PATH=${HELM_INSTALL_DIR}:${PATH}
                mkdir -p ${HELM_INSTALL_DIR} && \
                ./get_helm.sh
              '''
          
          // Prepare and validate Helm chart
          sh "sed -i \"s@^version:.*@version: ${globalDynamicVars.chartVersion}@g\" helmChart/Chart.yaml"
          sh "sed -i \"s@repository:.*@repository: ${globalDynamicVars.registryImageRepo}@g\" helmChart/values.yaml"
          sh "sed -i \"s@tag:.*@tag: ${globalDynamicVars.imageTag}@g\" helmChart/values.yaml"
          sh "${WORKSPACE}/helm_bin/helm lint --values helmChart/dev_values.yaml helmChart"
          
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
    stage('SonarQube') {
      steps {
        dir('complete') {
          script{
            def gitUrl = sh(script: 'git config remote.origin.url', returnStdout: true).trim()
            
            configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
              withSonarQubeEnv('sonarqube') {
                sh """\\
                  ./mvnw -s $MAVEN_SETTINGS org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.0.2155:sonar \\
                  -Dsonar.projectKey=${globalDynamicVars.appName} \\
                  -Dsonar.projectName=${globalDynamicVars.appName} \\
                  -Dsonar.projectVersion=${globalDynamicVars.appVersion} \\
                  -Dsonar.buildString=Jenkins-${env.BRANCH_NAME}-${env.BUILD_NUMBER} \\
                  -Dsonar.projectBaseDir=${WORKSPACE} \\
                  -Dsonar.links.ci=${BUILD_URL} \\
                  -Dsonar.links.scm=${gitUrl}
                """.stripIndent()
              }
            }
          }
          
          timeout(time: 3, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true
          }
        }
      }
    }
    stage('Deploy Dev') {
      when { branch 'develop' }
      steps {
        script {
          def appSecretJson=sh script: '${WORKSPACE}/aws_cli_bin/aws ssm get-parameter --name demo-secret --with-decryption', returnStdout: true
          def appSecretValue=readJSON(text: appSecretJson).Parameter.Value
          
          dir('complete/helmChart') {
            sh "${WORKSPACE}/helm_bin/helm upgrade ${globalDynamicVars.appName} --install --namespace dev-apps --create-namespace --atomic --set-string appSecret=\"${appSecretValue}\" --values dev_values.yaml ."
          }
        }
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
