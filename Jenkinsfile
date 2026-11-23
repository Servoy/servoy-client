pipeline {
    agent any
    
    options {
        quietPeriod(120)
        buildDiscarder(logRotator(daysToKeepStr: '40', numToKeepStr: '70'))
        
        // cancel all previous builds don't run at the same time
        disableConcurrentBuilds(abortPrevious: true)
    }
    
    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref']
            ],
            token: 'servoy-client',
            regexpFilterText: '$ref',
            regexpFilterExpression: "^refs/heads/${env.BRANCH}\$"
        )
    }

    parameters {
        // New boolean toggle for manual workspace wiping
        booleanParam(name: 'WIPE_WORKSPACE', defaultValue: false, description: 'Check this box to completely wipe the workspace BEFORE running the build.')

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
        stage('Clear Queued Builds') {
            steps {
                script {
                    // Annuleer builds die in de queue wachten op de quietPeriod timer voor dit specifieke pad
                    def currentJob = env.JOB_NAME
                    def queue = jenkins.model.Jenkins.get().queue
                    
                    queue.items.each { item ->
                        def queuedJobName = item.task.ownerTask?.fullName
                        if (queuedJobName == currentJob) {
                            echo "Removing pending queued build for ${currentJob} (Queue ID #${item.id})..."
                            queue.cancel(item)
                        }
                    }
                }
            }
        }

        // This stage executes if wipe is checked, cleans workspace, and re-triggers the job
        stage('Manual UI Workspace Wipe') {
            when {
                expression { params.WIPE_WORKSPACE }
            }
            steps {
                echo "Manual workspace wipe requested via UI toggle. Cleaning up..."
                cleanWs()
                
                echo "Re-triggering ${env.JOB_NAME} with WIPE_WORKSPACE = false..."
                build job: env.JOB_NAME, wait: false, parameters: [
                    booleanParam(name: 'WIPE_WORKSPACE', value: false),
                    string(name: 'goals', value: params.goals)
                ]
            }
        }

        // Only runs if WIPE_WORKSPACE is FALSE
        stage('Build with Tycho') {
            when {
                expression { !params.WIPE_WORKSPACE }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'master_mvn_repo', variable: 'MAVEN_SETTINGS'),
                    configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                ]) {
                    sh 'export MAVEN_OPTS="-Dmaven.test.failure.ignore=true" && mvn -B -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" $goals'
                }
            }
        }

        // JaCoCo report for the Java tests, same recipe as servoy-eclipse.
        // 'Build with Tycho' wrote target/jacoco.exec in every test module (servoy_ngclient.tests, servoy_mcp_tests).
        // The jacoco 'report' goal can only analyze the current module's own classes, so it runs in each PRODUCTION
        // bundle (-Pcoverage-report, root pom), which first merges the exec files of all test modules into
        // target/jacoco-merged.exec and then reports its own classes against it.
        // -Pplugins: activating a profile deactivates the activeByDefault one.
        stage('Coverage Report') {
            when {
                expression { !params.WIPE_WORKSPACE }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'master_mvn_repo', variable: 'MAVEN_SETTINGS'),
                    configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                ]) {
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        sh 'mvn -B -fae -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" -Pplugins,coverage-report org.jacoco:jacoco-maven-plugin:merge org.jacoco:jacoco-maven-plugin:report -pl servoy_base,servoy_shared,servoy_smart_client,servoy_headless_client,servoy_ngclient,servoy_mcp,servoy_debug'
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                if (!params.WIPE_WORKSPACE) {
                    // Specifieke testpaden voor servoy-client uit de oude configuratie
                    junit allowEmptyResults: true, testResults: 'servoy_ngclient/target/TEST*.xml,servoy_ngclient.tests/target/surefire-reports/*.xml'

                    // Java (JaCoCo) code coverage, one jacoco.xml per production bundle written by the
                    // 'Coverage Report' stage, aggregated here into a single Java coverage trend.
                    // sourceDirectories: jacoco.xml only holds package-relative file names and the plugin
                    // otherwise only looks in src/main/java, which these Eclipse bundles do not use.
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                        id: 'java-jacoco',
                        name: 'Java Coverage (JaCoCo)',
                        sourceCodeRetention: 'EVERY_BUILD',
                        sourceDirectories: [
                            'servoy_base', 'servoy_shared', 'servoy_smart_client', 'servoy_headless_client',
                            'servoy_ngclient', 'servoy_mcp', 'servoy_debug'
                        ].collect { [path: "${it}/src"] }
                    )

                    // Archive the per-bundle JaCoCo reports so they can be read per build (also via the Jenkins MCP).
                    // The binary jacoco*.exec files are not archived: they need the exact class files of this build.
                    archiveArtifacts artifacts: '**/target/site/jacoco/jacoco.xml', allowEmptyArchive: true, fingerprint: false
                    
                    // Jira Cloud integratie stap
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        jiraSendBuildInfo site: 'servoy-cloud.atlassian.net'
                    }
                }
            }
        }
        
        failure {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Failed', adaptiveCards: true
        }
        
        unstable {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Unstable', adaptiveCards: true
            script {
                if (!params.WIPE_WORKSPACE) {
                    build job: 'build', wait: false
                }
            }
        }
        
        fixed {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Back to Normal', adaptiveCards: true
        }
        
        success {
            script {
                if (!params.WIPE_WORKSPACE) {
                    build job: 'build', wait: false
                }
            }
        }
    }
}