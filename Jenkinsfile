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
    //DOCKER_REGISTRY_CREDS = credentials('docker_registry_creds')
    //GITLAB_TOKEN_CREDS = credentials('jx-pipeline-git-gitlab-gitlabms')
    SONARQUBE_PROJECT_NAME = "${env.APP_NAME}-${env.ENVIRONMENT_ID}"
    SONARQUBE_SERVER_ID = 'sonarqube'
  }
  stages {
    stage('Build') {
      steps {
        dir('complete') {
          script {
            def pomFile=readFile('pom.xml')
            def pom = new XmlParser().parseText(pomFile)
            
            globalDynamicVars.mvnArtifactId=pom['artifactId'].text().trim()
            globalDynamicVars.appVersion=pom['version'].text().trim()
            
            globalDynamicVars.imageTag="${globalDynamicVars.appVersion}-${BUILD_NUMBER}"
            globalDynamicVars.imageName=globalDynamicVars.mvnArtifactId.replaceAll('.', '-')
            globalDynamicVars.chartVersion=globalDynamicVars.imageTag.toLowerCase()
            globalDynamicVars.imageRepo=globalDynamicVars.imageName
            globalDynamicVars.registryImageRepo="${DOCKER_REGISTRY_REPO_URL}/${globalDynamicVars.imageRepo}"
            globalDynamicVars.image="${globalDynamicVars.registryImageRepo}:${globalDynamicVars.imageTag}"
          }
          
          commonLib_configHelmCharts chartDir: "charts/${APP_NAME}", imageTag: globalDynamicVars.imageTag, chartVersion: globalDynamicVars.chartVersion, registryImageRepo: globalDynamicVars.registryImageRepo
          
          commonLib_buildHelmChart chartDir: "charts/${APP_NAME}", helmRepoUrl: CHART_REPOSITORY, environmentId: 'sit'
          
          configFileProvider(
            [configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
            sh 'mvn -s $MAVEN_SETTINGS clean package'
          }
          
          sh "docker build --build-arg JAR_FILE=${globalDynamicVars.mvnArtifactId}-globalDynamicVars.appVersion.jar"
        }
      }
    }
  }
  /*
  post {
		changed {
			script {
				slackSend(
						color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
						channel: '#sagan-content',
						message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\n${env.BUILD_URL}")
		}
  }
  */
}
