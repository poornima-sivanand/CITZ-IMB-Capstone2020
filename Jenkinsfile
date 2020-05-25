pipeline {
    agent none
    options {
        disableResume()
    }
    
    stages {           
        stage('Build') {
            agent { label 'build' }
            steps {
                echo "Aborting all running jobs ..."
                script {
                    abortAllPreviousBuildInProgress(currentBuild)
                }
        
                script {    
                    // Use Pipeline-cli node project to build the open shift images, wiof-app-build ( open jdk image to build code with maven ) and wiof-build ( jboss web server image to host the web application ) 
                    echo "Building Openshift Images..." 
                    sh "cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run build -- --pr=${CHANGE_ID} --git.branch.name=${GIT_BRANCH} --git.branch.remote=${GIT_BRANCH} --git.branch.merge=${CHANGE_TARGET} --git.url=${GIT_URL}"
                   
                }
            }
        }

        stage('Deploy App Changes to DEV') {
            agent { label 'deploy' } // Run on jenkins slave 'build'
            steps {
                script {
                // Use Pipeline-cli node project to deploy the wiof-build image to Dev Stage 
                echo "Deploying to DEV ..."
                sh "cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run deploy -- --pr=${CHANGE_ID} --env=dev --git.branch.name=${GIT_BRANCH} --git.branch.remote=${GIT_BRANCH} --git.branch.merge=${CHANGE_TARGET} --git.url=${GIT_URL}" 
             }
           }
        }

        stage('Approval For TEST in Jira') {
            agent { label 'deploy' }   
            when {
                expression { return env.CHANGE_TARGET == 'master';}
                 beforeInput true;
            }  
            input {
                message "Are you ready to deploy to TEST?"
                id "deploy-TEST"
                parameters { }
            }
             steps {
                   script {
                      echo "Approved to go to Test"
               }
             }
         }
        
        stage('Deploy App Changes to TEST') {             
            agent { label 'deploy' } 
            when {
                // Run Stage only if Pull Request is to master branch
                expression { return env.CHANGE_TARGET == 'master';}
                beforeInput true;
            }                        
            steps {
                script {
                // Use Pipeline-cli node project to deploy the wiof-build image to Test Stage 
                echo "Deploying to Test ..."
                sh "cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run deploy -- --pr=${CHANGE_ID} --env=test --git.branch.name=${GIT_BRANCH} --git.branch.remote=${GIT_BRANCH} --git.branch.merge=${CHANGE_TARGET} --git.url=${GIT_URL}" 
            }
            }
        }



        stage('Approval For PROD') {
            agent { label 'deploy' }   
            when {
                expression { return env.CHANGE_TARGET == 'master';}
                 beforeInput true;
            }  
            input {
                message "Are you ready to deploy to PROD?"
                id "deploy-PROD"
                parameters { }
            }
             steps {
                   script {
                      echo "Approved to go to Prod"
               }
             }
         }

        stage('Deploy App Changes to PROD') {
            agent { label 'deploy' }
            when {
                // Run Stage only if Pull Request is to master branch
                expression { return env.CHANGE_TARGET == 'master';}
                beforeInput true;
            }      
            steps {
                script {
                // Use Pipeline-cli node project to deploy the wiof-build image to Prod Stage
                echo "Deploying to Prod ..."
                sh "cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run deploy -- --pr=${CHANGE_ID} --env=prod --git.branch.name=${GIT_BRANCH} --git.branch.remote=${GIT_BRANCH} --git.branch.merge=${CHANGE_TARGET} --git.url=${GIT_URL}"
                 }
              }
           }
        

       stage('Clean Out') {
            agent { label 'deploy' }   
            steps {
                // Use Pipeline-cli node project to clean openshift objects
                script {
                     sh "cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run clean -- --pr=${CHANGE_ID} --env=build --git.branch.name=${GIT_BRANCH} --git.branch.remote=${GIT_BRANCH} --git.branch.merge=${CHANGE_TARGET} --git.url=${GIT_URL}"
                     sh "cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run clean -- --pr=${CHANGE_ID} --env=dev --git.branch.name=${GIT_BRANCH} --git.branch.remote=${GIT_BRANCH} --git.branch.merge=${CHANGE_TARGET} --git.url=${GIT_URL}"
  
               }
            }
        }             
    }
}


