pipeline {
  agent any
  stages {
    stage('SCM') {
      steps {
        echo ' The SCM'
        script {
          if (params.TAG_VERSION == '') {
            error "TAG_VERSION is required"
          }

          echo params.TAG_VERSION

          checkout([
            $class: 'GitSCM',
            branches: [[name: "refs/tags/${params.TAG_VERSION}"]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[
              $class: 'SubmoduleOption',
              disableSubmodules: false,
              parentCredentials: false,
              recursiveSubmodules: false,
              reference: '',
              trackingSubmodules: false
            ]],
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: GIT_CREDENTIALS_ID, url: "https://github.com/kjh1234/azure-functions-samples-java.git"]]
            // userRemoteConfigs: [[credentialsId: GIT_CREDENTIALS_ID, url: "https://github.com/kjh1234/hello-spring-function-azure.git"]]
          ])
        }

      }
    }

    stage('Function Build/Deploy') {
      steps {
        withCredentials(bindings: [azureServicePrincipal(INNO_AZURE_CREDENTIALS)]) {
          sh """
                                                      az login --service-principal -u "\$AZURE_CLIENT_ID" -p "\$AZURE_CLIENT_SECRET" -t "\$AZURE_TENANT_ID"
                                                      az account set --subscription "\$AZURE_SUBSCRIPTION_ID"
                                                      
                                        	            chmod 764 ./mvnw
                                                      ./mvnw clean package
                                        	      
                                                      cd ${workspace}/target/azure-functions/inno-func-app && zip -r ../../../archive.zip ./* && cd -
                                                      az functionapp deployment source config-zip -g ${RESOURCE_GROUP} -n ${FUNC_NAME} --src ./archive.zip
                                                    """
        }

      }
    }

    stage('push Image') {
      steps {
        sh '''
        
        '''
      }
    }

    stage('Destroy') {
      steps {
        script {
          sh """
          az logout
          """
        }

      }
    }

  }
  environment {
    AZURE_SUBSCRIPTION_ID = credentials('AZURE_SUBSCRIPTION_ID')
    INNO_AZURE_CREDENTIALS = 'INNO_AZURE_CREDENTIALS'
    GIT_CREDENTIALS_ID = credentials('GIT_CREDENTIALS_ID')
    RESOURCE_GROUP = 'func-tf-jenkins'
    FUNC_NAME = 'inno-tf-func-app'
  }
  parameters {
    string(name: 'TAG_VERSION', defaultValue: '', description: '')
  }
}