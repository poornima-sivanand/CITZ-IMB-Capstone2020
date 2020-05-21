'use strict';
const {OpenShiftClientX} = require('../node_modules/@bcgov/pipeline-cli')
const path = require('path');

module.exports = async (settings) => {
  const phases = settings.phases
  const oc = new OpenShiftClientX(Object.assign({ namespace: phases.build.namespace }, settings.options));
  const phase = 'build';
  let objects = [];
  const templatesLocalBaseUrl = oc.toFileUrl(path.resolve(__dirname, '../../openshift'));

  objects.push(... oc.processDeploymentTemplate(`${templatesLocalBaseUrl}/jenkins.bc.json`, {
    'param':{
      'NAME': phases[phase].name,
      'SUFFIX': phases[phase].suffix,
      'VERSION': phases[phase].tag,
      'SOURCE_GIT_URL': oc.git.http_url,
      'SOURCE_GIT_REF': oc.git.branch.merge
    }
  }));

  oc.applyRecommendedLabels(objects, phases[phase].name, phase, phases[phase].changeId, phases[phase].instance);
  await oc.applyAndBuild(objects);
}