# Setup
#### 1. Copy and Paste .jenkins folder from https://github.com/bcgov/ocp-sso into your repository

#### 2. Alter the file .jenkins/.pipeline/package.json to ensure the following line points to the desired version of pipeline-cli npm module
    "pipeline-cli": "git+https://github.com/BCDevOps/pipeline-cli.git#release/v1.1"

#### 3. Under .jenkins/openshift folder, edit the following line in config.groovy file to point to the openshift namespace underwhich you want to install jenkins
      'build'{
            namespace = ''
            disposable = true
        }

#### 4. If using bitbucket, add Bitbucket secrets (Bitbucket username, Bitbucket password, Bitbucket URL) to the .jenkins/openshift/secrets.json file just like the github username, password that already exists

#### 5. Edit .jenkins/openshift/jenkins.dc.json file to include the bitbucket secrets if you added them

#### 6. Any additonal secrets/jenkins credentials can be added using steps 4 and 5. 
Create a secret to store bitbucket user credentials called bitbucket in Openshift Tools project

#### 7. Edit docker/Dockerfile to install any additional softwares or for customized configurations on jenkins

#### 8. Go to the current jenkins job configuration, https://jenkins-url/config.xml. Copy the contents and paste them in .jenkins/docker/contrib/configuration/jobs/<jobname>/config.xml file

#### 9. If running first time, run steps 9 and 10 or 11
```
oc run dev --image=docker-registry.default.svc:5000/bcgov/jenkins-basic:v2-latest -it --rm=true --restart=Never --command=true -- bash
#Wait for container to startuo and a shell to be available

```
#### 10. Getting Git
```
git clone --single-branch --depth 1 'https://github.com/BCDevOps/openshift-components.git' -b cvarjao-update-jenkins-basic /tmp/jenkins
```
#### 11. From local working directory
```
oc rsync 
```

#### 12. oc login
```
#perform oc login (Copy command from web console)
```

#### 13. Create openshift secrets provided under secrets.json
Use the provided `openshift/secrets.json` as follow:
```
oc -n wp9gel-tools process -f '../openshift/secrets.json' -p 'BITBUCKET_USERNAME=' -p 'BITBUCKET_PASSWORD='-p 'BITBUCKET_URL=' -p 'GH_USERNAME=' -p 'GH_PASSWORD=' | oc  -n wp9gel-tools create -f -
```

#### 14. Grant Admin access to Jenkins Service account in each managed namespace
```
oc -n bcgov policy add-role-to-user 'admin' 'system:serviceaccounts:bcgov-tools:jenkins'
oc -n bcgov-tools policy add-role-to-group 'system:image-puller' 'system:serviceaccounts:bcgov'
```

#### 15. Build : For first build and all subsequent builds
```
( cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run build -- --pr=#{CHANGE_ID} --dev-mode )
```

#### 16. Deploy : To deploy jenkins, since jenkins will always be deployed in tools project, the deb, test and prod environments need to be set to the same tools namespace on openshift. This can be changed in .jenkins/.pipeline/lib/config.js
```
( cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run deploy -- --pr=#{CHANGE_ID} --env=<dev,test or prod> )
```
#### To remove or undeploy/Cleanup
```
( cd .pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run clean -- --pr=#{CHANGE_ID} --env=<dev,test or prod> )
```

#### Add webade-properties, webade-jar secrets