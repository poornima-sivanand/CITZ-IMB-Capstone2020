# Setup
#### 1. Copy and Paste .jenkins folder from https://github.com/bcgov/ocp-sso into your repository

#### 2. Alter the file .jenkins/.jenkins/.pipeline/package.json to ensure the following line points to the desired version of pipeline-cli npm module
    "pipeline-cli": "git+https://github.com/BCDevOps/pipeline-cli.git#release/v1.1"

#### 3. Under .jenkins/openshift folder, edit the following line in config.groovy file to point to the openshift namespace underwhich you want to install jenkins
      'build'{
            namespace = ''
            disposable = true
        }


#### 4. Edit docker/Dockerfile to install any additional softwares or for customized configurations on jenkins


#### 5. If running first time, run steps 5,6,7

Login to Openshift from your command line.

```
oc run dev --image=docker-registry.default.svc:5000/bcgov/jenkins-basic:v2-latest -it --rm=true --restart=Never --command=true -- bash -n xordpe-tools

```

This would fetch the image into your namespace

#### 6. Create openshift secrets provided under secrets.json
Use the provided `openshift/secrets.json` as follow:
```
oc -n xordpe-tools process -f '../openshift/secrets.json'  -p 'GH_USERNAME=' -p 'GH_PASSWORD=' | oc  -n xordpe-tools create -f -
```

#### 7. Grant Admin access to Jenkins Service account in each managed namespace
```
oc -n xordpe-tools policy add-role-to-user 'admin' 'system:serviceaccounts:xordpe-tools:jenkins'
oc -n xordpe-tools policy add-role-to-group 'system:image-puller' 'system:serviceaccounts:xordpe-tools'
```

#### 8. Build : For first build and all subsequent builds
```
( cd .jenkins/.pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run build -- --pr=#{CHANGE_ID} --dev-mode=true )
```

Change-Id is used to distinguish paralled builds. You can use it sequentially.
Dev-mode is used because any change to jenkins needs to be deployed from the command prompt.

#### 9 Deploy : To deploy jenkins, since jenkins will always be deployed in tools project, the deb, test and prod environments need to be set to the same tools namespace on openshift. This can be changed in .jenkins/.jenkins/.pipeline/lib/config.js
```
( cd .jenkins/.pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run deploy -- --pr=#{CHANGE_ID} --env=<dev,test or prod> )
```
Since jenkins is deployed to *-tools namespace, the dev,test and prod will be in tools namespace

#### 10. Create a job manually once jenkins is deployed. Go to the current jenkins job configuration, https://jenkins-url/config.xml. Copy the contents and paste them in .jenkins/docker/contrib/configuration/jobs/<jobname>/config.xml file


#### 11. To remove or undeploy/Cleanup
```
( cd .jenkins/.pipeline && ${WORKSPACE}/npmw ci && DEBUG='info:*' ${WORKSPACE}/npmw run clean -- --pr=#{CHANGE_ID} --env=<dev,test> )
```
Cleanup can be used to clean the builds and deployments to dev and test to make sure you dont overutilize resources assigned to your namespace.