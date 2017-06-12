module.exports = function(config){
  config.set({
    basePath : '.',
    
    preprocessors: {
    	'../war/servoycore/**/*.html': ['ng-html2js'],    	
        '../war/servoydefault/**/*.html': ['ng-html2js']
    },
    files : [
       {pattern: 'fileResources/**/*', watched: true, included: false, served: true},
       
       // libraries for testing and angular
       'lib/jquery.js',
       'lib/phantomjs.polyfill.js',
       '../war/js/angular_1.*.js',
       'lib/angular-mocks*.js',
       
       // sablo and ngclient scripts
       'sablo/META-INF/resources/sablo/js/*.js', /* use this when running from Git */
       'sablo/META-INF/resources/sablo/*.js', /* use this when running from Git */
       '../war/js/numeral.js',
       '../war/js/**/*.js',
       
       // core components         
       '../war/servoycore/*/*.js',
       '../war/servoycore/*/*/*.js',       
       
       // components
       '../war/servoydefault/*/*.js',
       '../war/servoydefault/*/*/*.js',
       '../war/servoyservices/component_custom_property/*.js',
       '../war/servoyservices/custom_json_array_property/*.js',
       '../war/servoyservices/custom_json_object_property/*.js',
       '../war/servoyservices/foundset_linked_property/*.js',
       '../war/servoyservices/foundset_custom_property/*.js',
       '../war/servoyservices/foundset_viewport_module/*.js',

       // templates
       '../war/servoycore/**/*.html',       
       '../war/servoydefault/**/*.html',

       // tests
       'test/**/*.js'
    ],
    exclude : [
      '../war/servoycore/**/*_server.js',               
	  '../war/servoydefault/**/*_server.js',
	  '../war/js/**/*.min.js',
	  '../war/js/debug.js'
    ],
    ngHtml2JsPreprocessor: {
        // setting this option will create only a single module that contains templates
        // from all the files, so you can load them all with module('foo')
        moduleName: 'servoy-components',
        
        cacheIdFromPath: function(filepath) {
            return filepath.replace(/.*?\/servoydefault\/(.*)/,"servoydefault/$1").replace(/.*?\/servoycore\/(.*)/,"servoycore/$1");
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
        outputFile: '../../target/TEST-phantomjs-karma.xml'
  }
  /*,  alternative format
    junitReporter : {
      outputFile: 'test_out/unit.xml',
      suite: 'unit'
    }*/
  });
};
