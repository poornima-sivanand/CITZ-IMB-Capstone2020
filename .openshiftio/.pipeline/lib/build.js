'use strict';
const {OpenShiftClientX} = require('pipeline-cli')
const path = require('path');

module.exports = async (settings) => {
  const phases = settings.phases
  const oc = new OpenShiftClientX(Object.assign({ namespace: phases.build.namespace }, settings.options));
  const phase = 'build';
  let objects = [];
  const templatesLocalBaseUrl = oc.toFileUrl(path.resolve(__dirname, '../'));

  objects.push(... oc.processDeploymentTemplate(`${templatesLocalBaseUrl}/build.yaml`, {
    'param':{
      'NAME': phases[phase].name,
      'VERSION': phases[phase].tag,
      'SOURCE_GIT_URL': oc.git.http_url,
      'SOURCE_GIT_REF': oc.git.branch.name
    }
  }));

  oc.applyRecommendedLabels(objects, phases[phase].name, phase, phases[phase].changeId, phases[phase].instance);
  await oc.applyAndBuild(objects);
}