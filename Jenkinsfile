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
                    FORK_URL=sh(returnStdout: true, script: "echo ${GIT_URL} | sed s/bcgov/${CHANGE_FORK}/").trim()

                }
                script {    
                    VERSION="1.1"
                    // Use Pipeline-cli node project to build the open shift images, wiof-app-build ( open jdk image to build code with maven ) and wiof-build ( jboss web server image to host the web application ) 
                    echo "Building Openshift Images..." 
                    sh "cd .openshiftio/.pipeline && ././npmw ci && DEBUG=* ././npmw run build -- --pr=${CHANGE_ID} --git.branch.name=${CHANGE_BRANCH} --git.branch.merge=${CHANGE_BRANCH} --git.branch.remote=${CHANGE_BRANCH}--git.url=${FORK_URL}"
                }
            }
        }

        stage("Deploy App Changes to DEV") {
            agent { label "build" } // Run on jenkins slave "build"
            steps {
                script {
                // Use Pipeline-cli node project to deploy the wiof-build image to Dev Stage 
                echo "Deploying to DEV ..."
                sh "cd .openshiftio/.pipeline && ./npmw ci && DEBUG=* ./npmw run deploy -- --pr=${CHANGE_ID} --env=dev --git.branch.name=${CHANGE_BRANCH} --git.branch.merge=${CHANGE_BRANCH} --git.branch.remote=${CHANGE_BRANCH}--git.url=${FORK_URL}"
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
                sh "cd .openshiftio/.pipeline && ./npmw ci && DEBUG=* ./npmw run deploy -- --pr=${CHANGE_ID} --env=test --git.branch.name=${CHANGE_BRANCH} --git.branch.merge=${CHANGE_BRANCH} --git.branch.remote=${CHANGE_BRANCH}--git.url=${FORK_URL}"
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
                sh "cd .openshiftio/.pipeline && ./npmw ci && DEBUG=* ./npmw run deploy -- --pr=${CHANGE_ID} --env=prod --git.branch.name=${CHANGE_BRANCH} --git.branch.merge=${CHANGE_BRANCH} --git.branch.remote=${CHANGE_BRANCH}--git.url=${FORK_URL}"
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
                  sh "cd .openshiftio/.pipeline && ./npmw ci && DEBUG=* ./npmw run clean -- --pr=${CHANGE_ID} --env=dev --git.branch.name=${CHANGE_BRANCH} --git.branch.merge=${CHANGE_BRANCH} --git.branch.remote=${CHANGE_BRANCH}--git.url=${FORK_URL}"
                  sh "cd .openshiftio/.pipeline && ./npmw ci && DEBUG=* ./npmw run clean -- --pr=${CHANGE_ID} --env=build --git.branch.name=${CHANGE_BRANCH} --git.branch.merge=${CHANGE_BRANCH} --git.branch.remote=${CHANGE_BRANCH}--git.url=${FORK_URL}"

               }
            }
        }             
    }
}