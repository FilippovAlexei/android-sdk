pipeline {
  environment {
     APP_NAME = 'mindbox-android-sdk'
  }
   agent none
   stages {
        stage('Preflight check'){
            agent {node {label 'hetzner-agent-1' }}
            when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
            post {
                success {
                    slackSend channel: 'jenkins-mindbox', \
                    teamDomain: 'umbrellaitcom', \
                    color: '#5cb589', \
                    message: "MindBox » Android-SDK - ${env.JOB_BASE_NAME} ${env.BUILD_DISPLAY_NAME} Started (<${env.RUN_DISPLAY_URL}|Open>)",
                    tokenCredentialId: 'umbrella.devops-slack-integration-token'
                }
            }
            steps {sh "env | sort"}
        }

       stage('MindBox Android SDK tests'){
           when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
           parallel {
                stage ('Gradle Lint') {
                    when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
                    agent {node {label 'hetzner-agent-1' }}
                    steps {
                        gradle('check')
                    }
                }
                stage ('Unit Tests') {
                    when { anyOf { branch 'develop'; branch 'release'; branch 'pipeline'} }
                    agent {node {label 'hetzner-agent-1' }}
                    steps {
                        gradle('test')
                    }
                }
           }
        }

        stage('Build MindBox SDK'){
            agent {node {label 'hetzner-agent-1' }}
            when { anyOf { branch 'develop'; branch 'pipeline'} }
            steps {
                gradle('clean', 'build', 'assembleDebug', 'assembleAndroidTest')
            }
        }

   }
   post {
       success {
           slackSend channel: 'jenkins-mindbox', \
           teamDomain: 'umbrellaitcom', \
           color: '#5cb589', \
           message: "MindBox » Android-SDK - ${env.JOB_BASE_NAME} ${env.BUILD_DISPLAY_NAME} Success! (<${env.RUN_DISPLAY_URL}|Open>)", \
           tokenCredentialId: 'umbrella.devops-slack-integration-token'
       }
       failure {
           slackSend channel: 'jenkins-mindbox', \
           teamDomain: 'umbrellaitcom', \
           color: '#951d13', \
           message: "MindBox » Android-SDK - ${env.JOB_BASE_NAME} ${env.BUILD_DISPLAY_NAME} Failed! (<${env.RUN_DISPLAY_URL}|Open>)", \
           tokenCredentialId: 'umbrella.devops-slack-integration-token'
       }
   }
}
