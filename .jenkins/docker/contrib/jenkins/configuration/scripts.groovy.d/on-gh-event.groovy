import groovy.json.*
import jenkins.model.Jenkins 

String gitEvent = build.buildVariableResolver.resolve("x_event_key")
println gitEvent


 if (gitEvent == "pr:declined"  || gitEvent == "pr:deleted" || gitEvent == "pr:merged"){

String input = build.buildVariableResolver.resolve("payload")
String bitbucketUrl =  build.buildVariableResolver.resolve("bitbucket_url")
def pullRequestInfo = new JsonSlurper().parseText(input)
String changeId = pullRequestInfo.id
String gitRepo= pullRequestInfo.fromRef.repository.links.clone[0].href.replace('apps','bwa')
String author= pullRequestInfo.author.user.emailAddress
String app= pullRequestInfo.fromRef.repository.project.key.toUpperCase()
String appName = pullRequestInfo.fromRef.repository.project.key.toLowerCase()
String repoName = pullRequestInfo.fromRef.repository.slug
String commitId = pullRequestInfo.fromRef.latestCommit
String branchName =  pullRequestInfo.fromRef.displayId


println changeId + " " + gitRepo + " " + author + " " + appName + " " + commitId + " " + app + " " + branchName


 def credentials_store = jenkins.model.Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')
   credentials_store[0].credentials.each { it ->
    if (it instanceof com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl) {
        if ( it.id == "bitbucket-account") {
             gitUser=it.username
             user = it.username.replace('@','%40')
            pass = it.password
    }
    }

}
rmcommand = "rm -rf ${repoName}" 
def proc0=rmcommand.execute(null, new File("/tmp"))
def b0 = new StringBuffer()
proc0.consumeProcessErrorStream(b0)
println proc0.text
println b0.toString()


String gitR = gitRepo.replace('bwa',user+':'+pass+'@bwa')


def gitcommand = ["git", "clone", "${gitR}", "${repoName}"]
Process process2 = gitcommand.execute(null, new File("/tmp"))
process2.waitForProcessOutput()
def exitValue2 = process2.exitValue()
println ("Git clone completed with ${exitValue2}")

def checkoutcommand = ["git", "checkout", "--detach", "${commitId}"]
Process process3 = checkoutcommand.execute(null,new File("/tmp/${repoName}"))
process3.waitForProcessOutput()
def exitValue3 = process3.exitValue()
println ("Git checkout completed with ${exitValue3}")


def cleanInstallCommand = ["/tmp/${repoName}/npmw", "ci"]
Process process4 = cleanInstallCommand.execute(null, new File("/tmp/${repoName}/.pipeline"))
process4.waitForProcessOutput()
def exitValue4= process4.exitValue()
println ("npmw ci completed with ${exitValue4}")

StringBuffer stdout = new StringBuffer()
StringBuffer stderr = new StringBuffer()

def cleanDevCommand = ["/tmp/${repoName}/npmw", "run", "clean", "--", "--pr=${changeId}", "--env=dev", "--git.branch.name=${commitId}", "--git.branch.merge=${commitId}", "--git.branch.remote=${commitId}", "--git.url=${gitRepo}" ]
Process process5 = cleanDevCommand.execute(null, new File("/tmp/${repoName}/.pipeline"))
process5.waitForProcessOutput(stdout,stderr)
def exitValue5 = process5.exitValue()
println "${stdout}".toString()
println "${stderr}".toString()
println ("Clean Openshift Objects completed with ${exitValue5}")


def cleanBuildCommand = ["/tmp/${repoName}/npmw", "run", "clean", "--", "--pr=${changeId}", "--env=build", "--git.branch.name=${commitId}", "--git.branch.merge=${commitId}", "--git.branch.remote=${commitId}", "--git.url=${gitRepo}" ]
Process process6 = cleanBuildCommand.execute(null, new File("/tmp/${repoName}/.pipeline"))
process6.waitForProcessOutput(stdout,stderr)
def exitValue6 = process5.exitValue()
println "${stdout}".toString()
println "${stderr}".toString()
println ("Clean Openshift Objects completed with ${exitValue6}")




URL url = new URL("${bitbucketUrl}/rest/branch-utils/latest/projects/${app}/repos/${repoName}/branches");
HttpURLConnection connection = (HttpURLConnection)url.openConnection();
String userpass = gitUser + ":" + pass;
String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
connection.setRequestProperty ("Authorization", basicAuth);
connection.setRequestMethod("DELETE")
connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
connection.setRequestProperty("Accept", "application/json");
connection.setDoOutput(true);
String jsonInputString = "{\"name\": \"refs/heads/${branchName}\",\"dryRun\": false}"

OutputStream os = connection.getOutputStream()
    byte[] text = jsonInputString.getBytes("utf-8");
    os.write(text, 0, text.length);     
int responseCode = connection.getResponseCode();
if( responseCode == 204){
println ("Branch Deleted Successfully")
}
}
else{

println "This Event Handler job does not handle this event "
   
}
