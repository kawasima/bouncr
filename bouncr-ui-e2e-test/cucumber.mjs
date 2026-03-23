export default {
  requireModule: ['ts-node/register'],
  require: ['support/**/*.ts', 'steps/**/*.ts'],
  paths: ['features/**/*.feature'],
  format: ['progress-bar', 'html:reports/cucumber-report.html'],
  publishQuiet: true,
};
