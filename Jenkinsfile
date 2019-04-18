pipeline {
  agent {
    kubernetes {
      label 'ci-sample'
      yamlFile 'builder-pod.yaml'
    }
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
            def image = docker.build("nthienan/ci-sample:${env.BUILD_NUMBER}-dynamic")
            docker.withRegistry( '', 'nthienan_dockerhub') {
              image.push "${env.BUILD_NUMBER}-dynamic"
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
              sshCommand remote: aws_server, command: "docker rm -f ci-sample || true && docker run -d --name ci-sample -p 80:8080 nthienan/ci-sample:${env.BUILD_NUMBER}-dynamic"
            }
          }
        }
      }
    }
  }
}
