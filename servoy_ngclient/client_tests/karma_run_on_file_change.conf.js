module.exports = function(config){
  config.set({
    basePath : '.',

    files : [
       'lib/angular.js',
       'lib/angular-mocks.js',
       'lib/*',
       '../war/js/*',
       '../war/js/angularui/**',
       './test/*.js'
    ],

    exclude : [
	'../war/servoydefault/tabpanel/tabpanel_server.js'
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
