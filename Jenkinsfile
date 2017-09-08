pipeline {
    agent none

    environment {
        REPO_URL = "http://192.168.88.211:8081/artifactory"
    }

    stages {
        stage('Build') {
            agent {
                docker {
                    image 'docker-registry:5000/gradle'
                    label 'docker && ci4tma'
                }
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'gradle --info clean build sonarqube'
                }
            }
            post {
                always {
                    junit 'build/test-results/**/TEST-*.xml'
                }
                success {
                    archiveArtifacts 'build/libs/*.jar'
                    sh 'gradle artifactoryPublish'
                    echo "Built successfully"
                }
            }
        }
        stage('Containerize') {
            agent { label 'docker && ci4tma' }
            steps {
                echo 'Containerize'
                script {
                    def image = docker.build("docker-registry:5000/ci-sample:${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
                    image.push "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
                    image.push "${env.BRANCH_NAME}-latest"
                }
            }
        }
        stage('Deployment') {
            agent { label 'docker && ci4tma' }
            steps {
                echo 'Promotion'
                timeout(time:30, unit:'MINUTES') {
                    emailext (
                        subject: "[CI4TMA] Waiting approval for ${env.JOB_NAME} - build #${env.BUILD_NUMBER}",
                        body: """
                            Hi all,<br/><br/>
                            <p>${env.JOB_NAME} - build #${env.BUILD_NUMBER} is waiting your approval for deployment</p>
                            <p>Please go to: <a href='${env.BUILD_URL}'>${env.JOB_NAME} - build #${env.BUILD_NUMBER}</a> to approve</p><br/>
                            Thanks,<br/>
                            Build Team.
                        """,
                        to: "nthienan@tma.com.vn",
                        mimeType: "text/html"
                    )
                    input message:'Approve Deployment?'
                }
                sh "docker service update --image docker-registry:5000/ci-sample:${env.BRANCH_NAME}-${env.BUILD_NUMBER} ci-production"
            }
        }
    }
    post {
        success {
            emailext (
                subject: "[CI4TMA] ${env.JOB_NAME} - build #${env.BUILD_NUMBER}: SUCCESSFUL!",
                body: """
                    Dear All,<br/><br/>
                    <p>${env.JOB_NAME} - build #${env.BUILD_NUMBER}: SUCCESSFUL:</p>
                    <p>You can check console output at: <a href='${env.BUILD_URL}'>${env.JOB_NAME} - build #${env.BUILD_NUMBER}</a></p><br/>
                    Thanks,<br/>
                    Build Team.
                """,
                to: "nthienan@tma.com.vn",
                mimeType: "text/html"
            )
        }
        failure {
            emailext (
                subject: "[CI4TMA] ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}: FAILED!",
                body: """
                    Dear All,<br/><br/>
                    <p>${env.JOB_NAME} - build #${env.BUILD_NUMBER}: FAILED:</p>
                    <p>Please Check console output at: <a href='${env.BUILD_URL}'>${env.JOB_NAME} - build #${env.BUILD_NUMBER}</a></p><br/>
                    Thanks,<br/>
                    Build Team.
                """,
                to: "nthienan@tma.com.vn",
                mimeType: "text/html",
                attachLog: true
            )
        }
    }
}