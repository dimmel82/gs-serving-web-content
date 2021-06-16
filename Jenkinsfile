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
  tools {
    dockerTool 'docker' 
  }
  stages {
    stage('Build') {
      steps {
        dir('complete') {
          script {
            def pom=readMavenPom file: 'pom.xml'
            
            globalDynamicVars.mvnArtifactId=pom.artifactId
            globalDynamicVars.appVersion=pom.version
            
            globalDynamicVars.imageTag="${globalDynamicVars.appVersion}-${BUILD_NUMBER}"
            globalDynamicVars.imageName=globalDynamicVars.mvnArtifactId.replaceAll('.', '-')
            globalDynamicVars.chartVersion=globalDynamicVars.imageTag.toLowerCase()
            globalDynamicVars.imageRepo=globalDynamicVars.imageName
            globalDynamicVars.registryImageRepo="${DOCKER_REGISTRY_REPO_URL}/${globalDynamicVars.imageRepo}"
            globalDynamicVars.image="${globalDynamicVars.registryImageRepo}:${globalDynamicVars.imageTag}"
          }
          
          //commonLib_configHelmCharts chartDir: "charts/${APP_NAME}", imageTag: globalDynamicVars.imageTag, chartVersion: globalDynamicVars.chartVersion, registryImageRepo: globalDynamicVars.registryImageRepo
          
          //commonLib_buildHelmChart chartDir: "charts/${APP_NAME}", helmRepoUrl: CHART_REPOSITORY, environmentId: 'sit'
          
          sh 'chmod u+x mvnw'
          
          configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
            script {
              def ecrPassword=sh 'aws ecr get-login-password --region eu-west-1 --profile=default'
              withEnv(["DOCKER_PWD=${ecrPassword}"]) {
                sh "./mvnw -s $MAVEN_SETTINGS clean package docker:push -Ddocker.image.repo=${globalDynamicVars.registryImageRepo} -Ddocker.image.tag=${globalDynamicVars.imageTag} -Ddockerfile.username=AWS"
              }
            }
          }
        }
      }
    }
  }
  post {
		always {
      sleep 10000
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
