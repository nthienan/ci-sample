pipeline {
  agent {
    kubernetes {
      label 'ci-sample'
      yamlFile 'builder-pod.yaml'
    }
  }
  environment {
    ROLE_ID = "2ca933d8-5f7b-897e-650e-bcaf4f1e699b"
  }
  stages {
    stage('Build') {
      steps {
        container('gradle') {
          sh """
            gradle clean bootJar
          """
        }
      }
      post {
        success {
          sh """
            ls -la build/libs
          """
          stash includes: 'build/libs/ci-sample*.jar', name: 'app'
        }
      }
    }
    stage('Containerize') {
      steps {
        container('docker') {
          unstash 'app'
          script {
            def image = docker.build("nthienan/ci-sample:scenario3-${env.BUILD_NUMBER}")
            docker.withRegistry( '', 'nthienan_dockerhub') {
              image.push "scenario3-${env.BUILD_NUMBER}"
            }
          }
        }
      }
    }
    stage('Deploy') {
      steps {
        container('alpine') {
          withCredentials([usernamePassword(credentialsId: 'ssh_depoyment_server', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            script {
              def aws_server = [:]
              aws_server.name = "aws_server"
              aws_server.host = "server-1.nthienan.com"
              aws_server.allowAnyHosts = true
              aws_server.user = USERNAME
              aws_server.password = PASSWORD
              sshCommand remote: aws_server, command: "docker rm -f scenario-3 || true && docker run -d --name scenario-3 -p 8083:8080 nthienan/ci-sample:scenario3-${env.BUILD_NUMBER}"
            }
          }
        }
      }
    }
  }
}
