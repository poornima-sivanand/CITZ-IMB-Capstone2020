pipeline {
    agent none
    options {
        disableResume()
    }
    
    stages {           
        stage("Build") {
            agent { label "build" }
            steps {
                echo "Aborting all running jobs ..."
                script {
                    abortAllPreviousBuildInProgress(currentBuild)
                    echo "${GIT_URL1}:  ${GIT_URL} : ${GIT_URL2}: ${CHANGE_URL}"

                }
                script {    
                    VERSION="1.1"
                    // Use Pipeline-cli node project to build the open shift images, wiof-app-build ( open jdk image to build code with maven ) and wiof-build ( jboss web server image to host the web application ) 
                    echo "Building Openshift Images..." 
                    sh "oc process -f .openshiftio/build.yaml -p VERSION=${VERSION}-p SOURCE_GIT_URL=${CHANGE_URL} -p SOURCE_GIT_REF=${CHANGE_BRANCH} | oc apply --wait=true -n xordpe-tools -f -"
                    sh "oc -n xordpe-tools logs -f bc/capstone2020"
                }
            }
        }

        stage("Deploy App Changes to DEV") {
            agent { label "build" } // Run on jenkins slave "build"
            steps {
                script {
                // Use Pipeline-cli node project to deploy the wiof-build image to Dev Stage 
                echo "Deploying to DEV ..."
                sh "oc tag xordpe-tools/capstone2020-builder:${VERSION} xordpe-dev/capstone2020-builder:${VERSION}"
                sh "oc process -f .openshiftio/deployment.yaml -p VERSION=${VERSION}-p NAMESPACE=xordpe-dev | oc apply -n xordpe-dev -f -"
             }
           }
        }

         stage("Approval For Test") {
            agent { label "deploy" }   
            when {
                expression { return env.CHANGE_TARGET == "master";}
                 beforeInput true;
            }  
            input {
                message "Is the Test complete on dev?"
                parameters { }
                submitter "SYSTEM"
            }
             steps {
                   script {
                      echo "Approved"
               }
             }
         }


        stage("Deploy App Changes to TEST") {             
            agent { label "deploy" } 
            when {
                // Run Stage only if Pull Request is to master branch
                expression { return env.CHANGE_TARGET == "master";}
                beforeInput true;
            }                        
            steps {
                script {
                // Use Pipeline-cli node project to deploy the wiof-build image to Test Stage 
                echo "Deploying to Test ..."
                sh "oc tag xordpe-tools/capstone2020-builder:${VERSION} xordpe-test/capstone2020-builder:${VERSION}"
                sh "oc process -f .openshiftio/deployment.yaml -p VERSION=${VERSION} -p NAMESPACE=xordpe-test | oc apply -n xordpe-test -f -"
            }
            }
        }



        stage("Approval For PROD") {
            agent { label "deploy" }   
            when {
                expression { return env.CHANGE_TARGET == "master";}
                 beforeInput true;
            }  
            input {
                message "Is the test completed in Test Env?"
                parameters { }
                submitter "SYSTEM"
            }
             steps {
                   script {
                      echo "Approved"
               }
             }
         }

        stage("Deploy App Changes to PROD") {
            agent { label "deploy" }
            when {
                // Run Stage only if Pull Request is to master branch
                expression { return env.CHANGE_TARGET == "master";}
                beforeInput true;
            }      
            steps {
                script {
                // Use Pipeline-cli node project to deploy the wiof-build image to Prod Stage
                echo "Deploying to Prod ..."
                sh "oc tag xordpe-tools/capstone2020-builder:${VERSION} xordpe-prod/capstone2020-builder:${VERSION}"
                sh "oc process -f .openshiftio/deployment.yaml -p VERSION=${VERSION} -p NAMESPACE=xordpe-prod | oc apply -n xordpe-prod -f -"
                }
              }
           }
        

       stage("Clean Out") {
            agent { label "deploy" }   
            steps {
                // Use Pipeline-cli node project to clean openshift objects
                script {
               // Fetch all builds for the Pull request from JIRA and mark them succesful (possibility of multiple builds since passing Build keys through jenkins adds an unsucessful build as a Bug)
                  echo "Clean out"

               }
            }
        }             
    }
}