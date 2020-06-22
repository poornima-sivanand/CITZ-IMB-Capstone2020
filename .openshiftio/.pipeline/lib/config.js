'use strict';
const options= require('pipeline-cli').Util.parseArguments()
const changeId = options.pr //aka pull-request
const version = '1.0'
const name = 'capstone2020'

Object.assign(options.git, {owner: 'bcgov', repository: 'CITZ-IMB-CAPSTONE2020'})
const namespace = {build: 'xordpe-tools', dev: 'xordpe-dev', test: 'xordpe-test', prod: 'xordpe-prod'}

const phases0 = {
  namespace,
  name:      {build: `${name}`                     , dev: `${name}`                                                       , test: `${name}`                                             , prod: `${name}`},
  phase:     {build: 'build'                       , dev: 'dev'                                                           , test: 'test'                                                , prod: 'prod'},
  changeId:  {build: changeId                      , dev: changeId                                                        , test: changeId                                              , prod: changeId},
  tag:       {build: `build-${version}-${changeId}`, dev: `dev-${version}-${changeId}`                                    , test: `test-${version}`                                     , prod: `prod-${version}`},
  instance:  {build: `${name}-build-${changeId}`   , dev: `${name}-dev-${changeId}`                                       , test: `${name}-test`                                        , prod: `${name}-prod-${changeId}`},
  host:      {build: ''                            , dev: `${name}-${namespace.dev}.pathfinder.gov.bc.ca`                 , test: `${name}-${namespace.test}.pathfinder.gov.bc.ca`      , prod: `${name}-${namespace.prod}.pathfinder.gov.bc.ca`},
  transient: {build: true                          , dev: true                                                            , test: false                                                 , prod: false},
};

const phases = {};
// Pivot configuration table, so that `phase name` becomes a top-level property
// { namespace: { build: '-tools',  dev: '-dev'}}   ->  { build: { namespace: '-tools' }, dev: { namespace: '-dev' } }
Object.keys(phases0).forEach((properyName) => {
  const property = phases0[properyName];
  Object.keys(property).forEach((phaseName) => {
    phases[phaseName] = phases[phaseName] || {};
    phases[phaseName][properyName] = property[phaseName];
  });
});

process.on('unhandledRejection', (reason) => {
  console.log(reason);
  process.exit(1);
});

module.exports = exports = {phases, options};