pipeline {
    agent any
    
    options {
        quietPeriod(120)
        buildDiscarder(logRotator(daysToKeepStr: '40', numToKeepStr: '70'))
    }
    
    triggers {
        githubPush()
    }
    
    parameters {
        string(name: 'goals', defaultValue: 'clean install', trim: false)
    }
    
    environment {
        // Haal de webhook URL veilig op uit de Jenkins kluis
        TEAMS_WEBHOOK = credentials('servoy-teams-webhook')
    }
    
    tools {
        jdk 'Java 21'
        maven 'Maven 3.9.16'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build with Tycho 5') {
            steps {
                configFileProvider([
                    configFile(fileId: 'master_mvn_repo', variable: 'MAVEN_SETTINGS'),
                    configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                ]) {
                    // MAVEN_OPTS toevoegen om testfouten te negeren zoals in je originele config
                    sh 'export MAVEN_OPTS="-Dmaven.test.failure.ignore=true" && mvn -B -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" $goals'
                }
            }
        }
    }
    
    post {
        always {
            // Specifieke testpaden voor servoy-client uit de oude configuratie
            junit allowEmptyResults: true, testResults: 'servoy_ngclient/target/TEST*.xml,servoy_ngclient.tests/target/surefire-reports/*.xml'
            
            // Jira Cloud integratie stap
            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                jiraSendBuildInfo site: 'servoy-cloud.atlassian.net'
            }
        }
        
        failure {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Failed'
        }
        
        unstable {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Unstable'
            build job: 'build', wait: false
        }
        
        fixed {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Back to Normal'
        }
        
        success {
            build job: 'build', wait: false
        }
    }
}