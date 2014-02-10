module.exports = function(config){
  config.set({
    basePath : '.',

    files : [
        '../war/js/angular.js',
        '../war/js/*',
        '../war/js/angularui/**',
        'lib/angular-mocks.js',
        './test/*.js'
    ],

    exclude : [
      /*'app/lib/angular/angular-loader.js',
      'app/lib/angular/*.min.js',
      'app/lib/angular/angular-scenario.js'*/
    ],

    autoWatch : true,

    frameworks: ['jasmine'],

    browsers : ['PhantomJS'],

    reporters: ['dots', 'junit'],
    junitReporter: {
    outputFile: 'test-results.xml'
    }
  });
};
