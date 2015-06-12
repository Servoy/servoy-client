module.exports = function(config){
  config.set({
    basePath : '.',
    
    preprocessors: {
        '../war/servoydefault/**/*.html': ['ng-html2js']
    },
    files : [
       {pattern: 'fileResources/**/*', watched: true, included: false, served: true},
       'lib/jquery.js',
       'lib/angular_1.4.0b5.js',
       'lib/angular-mocks_1.4.0b5.js',
       '../war/js/numeral.js',
       /*'../../../sablo/sablo/META-INF/resources/sablo/js/*.js', /* use this when running from Git */
       '../../sablo/META-INF/resources/sablo/js/*.js',  /* use this when running from SVN-git bridge */
       'lib/phantomjs.polyfill.js',
       '../war/js/**/*.js',
       '../war/servoydefault/*/*.js',
       '../war/servoydefault/portal/js/ui-grid.js',
       './test/**/*.js',
       '../war/servoydefault/*/*.html',
       '../war/servoyservices/component_custom_property/*.js',
       '../war/servoyservices/custom_json_array_property/*.js',
       '../war/servoyservices/foundset_custom_property/*.js',
       '../war/servoyservices/foundset_viewport_module/*.js'
    ],
    exclude : [
	  '../war/servoydefault/tabpanel/tabpanel_server.js',
	  '../war/servoydefault/splitpane/splitpane_server.js',
	  '../war/servoydefault/portal/portal_server.js',
	  '../war/js/**/*.min.js',
	  '../war/js/**/angular1.3.4.js'
      /*'app/lib/angular/angular-loader.js',
      'app/lib/angular/*.min.js',
      'app/lib/angular/angular-scenario.js'*/
    ],
    ngHtml2JsPreprocessor: {
        // setting this option will create only a single module that contains templates
        // from all the files, so you can load them all with module('foo')
        moduleName: 'servoy-components',
        
        cacheIdFromPath: function(filepath) {
            return filepath.replace(/.*?\/servoydefault\/(.*)/,"servoydefault/$1");
        },
    },

    frameworks: ['jasmine'],
    browsers : ['PhantomJS'],//

    /*plugins : [    <- not needed since karma loads by default all sibling plugins that start with karma-*
            'karma-junit-reporter',
            'karma-chrome-launcher',
            'karma-firefox-launcher',
            'karma-script-launcher',
            'karma-jasmine'
            ],*/
    singleRun: true,
    //autoWatch : true,
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
