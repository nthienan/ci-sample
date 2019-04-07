pipeline {
  agent {
    kubernetes {
      label 'ci-sample'
      yamlFile 'builder-pod.yaml'
    }
  }
  environment {
    ROLE_ID = "94cf2daf-77a5-d475-8475-713ee90e31e3"
  }
  stages {
    stage('Build') {
      steps {
        container('gradle') {
          sh """
            gradle clean build
          """
        }
      }
      post {
        success {
          sh """
            ls -la build
            ls -la build/libs
          """
          stash includes: 'build/libs/ci-sample*.jar', name: 'app'
        }
      }
    }
    stage('Containerize') {
      steps {
        container('docker') {
          sh """
            docker build -t nthienan/ci-sample .
          """
        }
      }
    }
    stage('Deploy') {
      steps {
        container('vault') {
          withCredentials([string(credentialsId: 'jenkins_vault_token', variable: 'VAULT_TOKEN')]) {
            sh """
              set +x
              echo $HOSTNAME
              export VAULT_ADDR=http://vault.default.svc.cluster.local
              SECRET_ID=`vault write -field=secret_id -f auth/approle/role/team-a/secret-id`
              VAULT_TOKEN=`vault write -field=token auth/approle/login role_id=$ROLE_ID secret_id=\$SECRET_ID`
              vault kv get -field=username kv/team-a/mongodb
              USERNAME=`vault kv get -field=username kv/team-a/mongodb`
              PASSWORD=`vault kv get -field=password kv/team-a/mongodb`
              echo -n "username=\$USERNAME\npassword=\$PASSWORD" > config.properties
            """
          }
        }
      }
      post {
        success {
          archiveArtifacts artifacts: 'config.properties'
        }
      }
    }
  }
}
