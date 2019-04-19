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
            def image = docker.build("nthienan/ci-sample:${env.BUILD_NUMBER}-k8s")
            docker.withRegistry( '', 'nthienan_dockerhub') {
              image.push "${env.BUILD_NUMBER}-k8s"
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
            sed -i -e 's,APPLICATION_IMAGE,'nthienan/ci-sample:${env.BUILD_NUMBER}-k8s',g' \$deployment_file
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
