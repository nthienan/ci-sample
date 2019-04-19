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
            def image = docker.build("nthienan/ci-sample:scenario2-${env.BUILD_NUMBER}")
            docker.withRegistry( '', 'nthienan_dockerhub') {
              image.push "scenario2-${env.BUILD_NUMBER}"
            }
          }
        }
      }
    }
    stage('Deploy') {
      steps {
        container('alpine') {
          sh """
            deployment_file="./deployment.yaml"
            sed -i -e 's,APPLICATION_IMAGE,'nthienan/ci-sample:scenario2-${env.BUILD_NUMBER}',g' \$deployment_file
            cat \$deployment_file
          """
          stash includes: 'deployment.yaml', name: 'deployment_config'
        }
        container('kubectl') {
          unstash 'deployment_config'
          withKubeConfig([credentialsId: 'k8s_token', serverUrl: 'https://35.240.212.207']) {
            sh """
              kubectl apply -f deployment.yaml
            """
          }
        }
      }
    }
  }
}
