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

    /*autoWatch : true,*/

    frameworks: ['jasmine'],

    browsers : ['PhantomJS'],

    /*plugins : [    <- not needed since karma loads by default all sibling plugins that start with karma-*
            'karma-junit-reporter',
            'karma-chrome-launcher',
            'karma-firefox-launcher',
            'karma-script-launcher',
            'karma-jasmine'
            ],*/
    singleRun: true,
            reporters: ['dots', 'junit'],
            junitReporter: {
            outputFile: 'test-results.xml'
    }
  /*,  alternative format
    junitReporter : {
      outputFile: 'test_out/unit.xml',
      suite: 'unit'
    }*/
  });
};
