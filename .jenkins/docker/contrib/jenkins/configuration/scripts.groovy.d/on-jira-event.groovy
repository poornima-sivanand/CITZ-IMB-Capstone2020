import groovy.json.*
import jenkins.model.Jenkins 

// Class definition for OnJiraEvent
class OnJiraEvent extends Script {
    static Map exec(List args, File workingDirectory=null, Appendable stdout=null, Appendable stderr=null, Closure stdin=null){
        ProcessBuilder builder = new ProcessBuilder(args as String[])
        if (stderr ==null){
            builder.redirectErrorStream(true)
        }
        if (workingDirectory!=null){
            builder.directory(workingDirectory)
        }
        def proc = builder.start()
        if (stdin!=null) {
            OutputStream out = proc.getOutputStream();
            stdin(out)
            out.flush();
            out.close();
        }
        if (stdout == null ){
            stdout = new StringBuffer()
        }
        proc.waitForProcessOutput(stdout, stderr)
        int exitValue= proc.exitValue()
        Map ret = ['out': stdout, 'err': stderr, 'status':exitValue, 'cmd':args]
        return ret
    }
   // Get jenkins stored credentials for accessing jira and bitbucket
    def getCredentials(){
        def credentials_array = []
        def credentials_store = jenkins.model.Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')
        credentials_store[0].credentials.each { it ->
          if (it instanceof com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl) {
              if ( it.id == "bitbucket-account") {
                  user = it.username
                  pass = it.password
               }
            }
            credentials_array.push(user)
            credentials_array.push(pass)
        }
        return credentials_array
    }
    // get jira task information
    def getTaskInfo(String task){
           def credentials_array=this.getCredentials()
           URL url = new URL('https://bwa.nrs.gov.bc.ca/int/jira/rest/api/2/issue/'+task);
           HttpURLConnection connection = (HttpURLConnection)url.openConnection();
           String userpass = credentials_array[0]+ ":" + credentials_array[1];
           String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
           connection.setRequestProperty ("Authorization", basicAuth);
           connection.requestMethod = "GET"
           connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
           BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
           String line;
           StringBuilder result = new StringBuilder();
           while ((line = rd.readLine()) != null) {
                  result.append(line);
            }  
           result1=result.toString();
           def taskInfo=new JsonSlurper().parseText(result1)
           return taskInfo
    }

    def getRepoAndBranchInfo(def rfcKey){
        // Query RFC Info
        def rfcInfo = this.getTaskInfo(rfcKey)
        def issuelinks = rfcInfo.fields.issuelinks
        def repoName = ''
        def branchName = ''
        def changeId = ''
        def prNo = ''
        def repoBranchArray = []
        // Fetch RFC Key linked to RFD
        for (int i=0; i<issuelinks.size();i++){
            if(issuelinks[i].type.name=="RFC-RFD"){
                def rfdKey = issuelinks[i].outwardIssue.key
                def rfdInfo = this.getTaskInfo(rfdKey)
                def labels = rfdInfo.fields.labels
                if(labels.contains("auto")){
                    
                    def branchSummary = rfdInfo.fields.summary.split('-')
                    branchName = branchSummary.drop(2).dropRight(2).join('-')
                    rfdSubtaskKey = rfdInfo.fields.subtasks.key
                    rfdSubtaskInfo = this.getTaskInfo(rfdSubtaskKey)
                    repoName = rfdSubtaskInfo.fields.components.name
                    changeId = branchSummary.takeRight(1).toString()
                    prNo = branchSummary.takeRight(2).join('-')
                    repoBranchArray.push(repoName)
                    repoBranchArray.push(branchName)
                    repoBranchArray.push(changeId)
                    repoBranchArray.push(prNo)
                    break;
                }
            }
            else{
                //pass
            }
        }
        return repoBranchArray
       }
    // Main code: Function to handle Jira Events
    def run() {
        
        //Fetch input Payload from Incoming Event
        String input = build.buildVariableResolver.resolve("payload")
        // ****** Check if payload is not null ******
        if (input?.trim()){
            def payload = new JsonSlurper().parseText(input)
            def issuetype = payload.fields.issuetype.name
            def issueStatus = payload.fields.status.name
            def projectName = payload.fields.project.key
            def issueInfo = ''
            def rfcKey = ''
            def rfcInfo = ''
            def rfdInfo = ''
            def rfdKey = ''
            def rfdSubtaskKey = ''
            def rfdSubtaskInfo = ''
            def repoName = ''
            def branchName = ''
            def rfcenv = ''
            def repoBranchInfo = ''
            def prNo = ''
            def prevEnv = "nil"
            def changeId = ''
            // ****** Check if issue type is RFD or RFC ******
            if(issuetype == "RFD" || issuetype == "RFC" ) {
                def env = ""
                println "${issuetype} : ${issueStatus}"
                if ( issuetype == "RFD") {
                    issueInfo = payload.fields.summary
                    env = payload.fields.customfield_10121.value.toLowerCase()
                    issuelinks = payload.fields.issuelinks
                    // Fetch RFC Key linked to RFD
                    for (int i=0; i<issuelinks.size();i++){
                        if(issuelinks[i].type.name=="RFC-RFD"){
                            rfcKey = issuelinks[i].inwardIssue.key
                            break;
                        }
                        else{
                            //pass 
                        }
                    }
                    repoBranchInfo = this.getRepoAndBranchInfo(rfcKey)
                }
                else {
                    if (issueStatus!="Closed"){
                        env = issueStatus.split(" ")[2]
                        rfcenv = env
                        if(env.toLowerCase() == "int"){
                            env = "dlvr"
                        }       
                    }
                    repoBranchInfo = this.getRepoAndBranchInfo(payload.key)
                }
                repoName = repoBranchInfo[0].join("")
                branchName = repoBranchInfo[1]
                changeId = repoBranchInfo[2]-"["-"]"
                prNo = repoBranchInfo[3]
                env = env.toLowerCase()
                rfcenv = rfcenv.toLowerCase()
                def issueStatusL = issueStatus.toLowerCase()
                if ( (issuetype == "RFD" && (issueStatus == "Approved" || ( env.toLowerCase()=="dlvr" || env.toLowerCase()=="test" && issueStatus == "Closed"))) || (issuetype == "RFC" && issueStatusL == "authorized for ${rfcenv}")){
                    if( issuetype == "RFD" && issueStatus == "Closed"){
                        if(env.toLowerCase()=="dlvr"){
                            env="test"
                        }
                        else if(env.toLowerCase() == "test"){
                            env="prod"
                        }


                    }
                    
                    def owner=projectName
                    println "Reponame: ${repoName}, ProjectName: ${projectName}, BranchName: ${branchName}"
                    
                    def credentials_array = this.getCredentials()
                    def gitUser= credentials_array[0].replace('@','%40')
                    def pass = credentials_array[1]
                    String gitRepo = "https://${gitUser}:${pass}@bwa.nrs.gov.bc.ca/int/stash/scm/${projectName}/${repoName}.git"
                    String gitUrl = "https://bwa.nrs.gov.bc.ca/int/stash/scm/${projectName}/${repoName}.git"

                    def sout = new StringBuilder(), serr = new StringBuilder()
                    def lscommand = ["ls", "-ld","${repoName}-${changeId}"]
                    Process process1 = lscommand.execute(null, new File("/tmp"))
                    process1.waitForProcessOutput(sout,serr)
                    def exitValue1 = process1.exitValue()
                    println ("Ls completed with ${exitValue1}")
                                        
                    if(serr){
                    //Clone Git Repo 
                    def gitcommand = ["git", "clone", "${gitRepo}", "${repoName}-${changeId}"]
                    Process process2 = gitcommand.execute(null, new File("/tmp"))
                    process2.waitForProcessOutput()
                    def exitValue2 = process2.exitValue()
                    println ("Git clone completed with ${exitValue2}")

                    // Checkout Git Branch
                    def checkoutcommand = ["git", "checkout", "${branchName}"]
                    Process process3 = checkoutcommand.execute(null,new File("/tmp/${repoName}-${changeId}"))
                    process3.waitForProcessOutput()
                    def exitValue3 = process3.exitValue()
                    println ("Git checkout completed with ${exitValue3}")
                    }

                    // Install node modules
                    def installCommand = ["/tmp/${repoName}-${changeId}/npmw", "install"]
                    Process process4 = installCommand.execute(null, new File("/tmp/${repoName}-${changeId}/.pipeline"))
                    process4.waitForProcessOutput()
                    def exitValue4= process4.exitValue()
                    println ("npmw ci completed with ${exitValue4}")

                    // Run Verfication
                    def verifyCommand = ["/tmp/${repoName}-${changeId}/npmw", "run", "verify-before-deploy","--", "--pr=${changeId}", "--env=${env}", "--git.branch.name=${branchName}", "--git.branch.merge=${branchName}", "--git.branch.remote=${branchName}", "--git.url=${gitUrl}"]
                    Process process5 = verifyCommand.execute(null, new File("/tmp/${repoName}-${changeId}/.pipeline"))
                    def output= process5.in.text;
                    println (output)
                    process5.waitForProcessOutput()
                    def exitValue5= process5.exitValue()
                    println ("npmw run verify-before-deploy completed with ${exitValue5}")

                   
                    def deploymentReadiness = output.toString()
                    def status = ''
                    def temp = deploymentReadiness.split("\n").takeRight(1).join(" ")
                    def tempString = temp.split(" ").takeRight(2)
                    if ( tempString.contains("NOT")){
                        status = tempString.takeRight(2).join(" ")
                    }
                    else {
                        status = tempString.takeRight(1).join(" ")
                    }
                    //println status
                    if(status == "READY"){
                        String jobName1= repoName;
                        println jobName1
                        List projects = []
                        projects = jenkins.model.Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject.class).findAll {
                            def scmSource=it.getSCMSources()[0]
                            return owner.equalsIgnoreCase(scmSource.getRepoOwner()) ||  owner.equalsIgnoreCase("SAMPLE") ||  owner.equalsIgnoreCase("ZERO")  && jobName1.equalsIgnoreCase(scmSource.getRepository())
                            }
                        println projects
                        List branchProjects = []
                        projects.each {
                            def branchProject = it.getItem(prNo)
                            if (branchProject!=null){
                                branchProjects.add(branchProject)
                            }
                        }
            
                        println branchProjects
                        if (branchProjects.size() > 0){
                            branchProjects.each { targetJob ->
                            if (targetJob.getLastBuild()){
                                hudson.security.ACL.impersonate(hudson.security.ACL.SYSTEM, {
                                    for (org.jenkinsci.plugins.workflow.support.steps.input.InputAction inputAction : targetJob.getLastBuild().getActions(org.jenkinsci.plugins.workflow.support.steps.input.InputAction.class)){
                                        for (org.jenkinsci.plugins.workflow.support.steps.input.InputStepExecution inputStep:inputAction.getExecutions()){
                                            println inputStep.getId()
                                            def envId=env.toUpperCase()
                                            if (!inputStep.isSettled() && inputStep.getId()=="Jira-${envId}"){
                                                println inputStep.proceed(null)
                                            }
                                        }
                                    }
                                } as Runnable )
                            }
                          }
                        }
                        else {
                            println "There is no project or build associated"
                        }
                    } 

                }
                else if ((issuetype == "RFC" && issueStatus == "Closed")||((issuetype == "RFD" && env.toLowerCase()=="prod" && issueStatus == "Closed"))){
                     def owner=projectName
                    println "Reponame: ${repoName}, ProjectName: ${projectName}, BranchName: ${branchName}"
                    
                    def credentials_array = this.getCredentials()
                    def gitUser= credentials_array[0].replace('@','%40')
                    def pass = credentials_array[1]
                    String gitRepo = "https://${gitUser}:${pass}@bwa.nrs.gov.bc.ca/int/stash/scm/${projectName}/${repoName}.git"
                    String gitUrl = "https://bwa.nrs.gov.bc.ca/int/stash/scm/${projectName}/${repoName}.git"

                    def sout = new StringBuilder(), serr = new StringBuilder()
                    def lscommand = ["ls", "-ld","${repoName}-${changeId}"]
                    Process process6 = lscommand.execute(null, new File("/tmp"))
                    process6.waitForProcessOutput(sout,serr)
                    def exitValue6 = process6.exitValue()
                    println ("Ls completed with ${exitValue6}")
                                        
                    if(serr){
                    //Clone Git Repo 
                    def gitcommand = ["git", "clone", "${gitRepo}", "${repoName}-${changeId}"]
                    Process process7 = gitcommand.execute(null, new File("/tmp"))
                    process7.waitForProcessOutput()
                    def exitValue7 = process7.exitValue()
                    println ("Git clone completed with ${exitValue7}")

                    // Checkout Git Branch
                    def checkoutcommand = ["git", "checkout", "${branchName}"]
                    Process process8 = checkoutcommand.execute(null,new File("/tmp/${repoName}-${changeId}"))
                    process8.waitForProcessOutput()
                    def exitValue8 = process8.exitValue()
                    println ("Git checkout completed with ${exitValue8}")
                    }
                   

                    // Install node modules
                    def installCommand = ["/tmp/${repoName}-${changeId}/npmw", "install"]
                    Process process9 = installCommand.execute(null, new File("/tmp/${repoName}-${changeId}/.pipeline"))
                    process9.waitForProcessOutput()
                    def exitValue9= process9.exitValue()
                    println ("npmw ci completed with ${exitValue9}")

                    def verifyBeforeMerge =  ["/tmp/${repoName}-${changeId}/npmw", "run", "verify-before-merge", "--", "--pr=${changeId}",  "--repoName=${repoName}", "--projectName=${projectName}", "--git.branch.merge=${branchName}"]
                    Process process10 = verifyBeforeMerge.execute(null, new File("/tmp/${repoName}-${changeId}/.pipeline"))
                    def output2= process10.in.text;
                    println (output2)
                    process10.waitForProcessOutput()
                    def exitValue10= process10.exitValue()
                    println ("npmw run verify before merge completed with ${exitValue10}")
                    def mergeReadiness = output2.toString()
                    def status = ''
                    def temp = mergeReadiness.split("\n").takeRight(1).join(" ")
                    def tempString = temp.split(" ").takeRight(2)
                    if ( tempString.contains("NOT")){
                        status = tempString.takeRight(2).join(" ")
                    }
                    else {
                        status = tempString.takeRight(1).join(" ")
                    }
                    //println status
                    if(status == "READY"){

                    def mergePR =  ["/tmp/${repoName}-${changeId}/npmw", "run", "merge-pr", "--", "--pr=${changeId}",  "--repoName=${repoName}", "--projectName=${projectName}", "--git.branch.merge=${branchName}"]
                    Process process11 = mergePR.execute(null, new File("/tmp/${repoName}-${changeId}/.pipeline"))
                    def output3= process11.in.text;
                    println (output3)
                    process11.waitForProcessOutput()
                    def exitValue11= process11.exitValue()
                    println ("npmw run merge-PR completed with ${exitValue11}")

                      //Remove stale folders
                     def rmcommand = ["sh", "-c", "rm -rf *-ear-*"]  
                    Process process12=rmcommand.execute(null, new File("/tmp"))
                    process12.waitForProcessOutput()
                    def exitValue12= process12.exitValue()
                    println ("Clean jenkins env completed with ${exitValue12}") 

               }
               else {

                  println("Not Ready for Merge")
                  }

                }
                else { 
                     println "This job only executes when automated RFDs are approved or RFCs are authorized or closed"
                }
        
            }
            
            else {
               println "This issue type is not handled by this job"
            }
        }
        else {
           println "Payload is null"
        }
        return null; //end run
    }
    static void main(String[] args) {
        org.codehaus.groovy.runtime.InvokerHelper.runScript(OnJiraEvent, args)     
    }
}

    