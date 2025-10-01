module.exports = function(config) {
	config.set({
		basePath: '.',

		preprocessors: {
			'../war/servoycore/**/*.html': ['ng-html2js'],
			'../war/servoydefault/**/*.html': ['ng-html2js']
		},
		files: [
			{ pattern: 'fileResources/**/*', watched: true, included: false, served: true },

			// libraries for testing and angular
			'lib/jquery.js',
			'lib/phantomjs.polyfill.js',
			'../war/js/angular.js',
			'lib/angular-mocks.js',

			// sablo scripts
			'sablo/META-INF/resources/sablo/js/*.js', /* use this when running from Git */
			'sablo/META-INF/resources/sablo/types/*.js', /* use this when running from Git */
			'sablo/META-INF/resources/sablo/*.js', /* use this when running from Git */

			// ngclient scripts
			'../war/js/numeral.js',
			'../war/js/**/*.js',

			// core components         
			'../war/servoycore/*/*.js',
			'../war/servoycore/*/*/*.js',

			// components
			'../war/servoydefault/*/*.js',
			'../war/servoydefault/*/*/*.js',
			'../war/servoyservices/foundset_viewport_module/*.js',
			'../war/servoyservices/foundset_custom_property/*.js',
			'../war/servoyservices/foundset_linked_property/*.js',
			'../war/servoyservices/component_custom_property/*.js',

			// templates
			'../war/servoycore/**/*.html',
			'../war/servoydefault/**/*.html',

			// tests
            'test/**/*.js'
		],
		exclude: [
            '../war/servoy_ng_only_services/**/*_server.js',
            '../war/servoy_ng_only_services/**/*_doc.js',
			'../war/servoycore/**/*_server.js',
            '../war/servoycore/**/*_doc.js',
			'../war/servoydefault/**/*_server.js',
            '../war/servoydefault/**/*_doc.js',
			'../war/js/**/*.min.js',
			'../war/js/debug.js'
		],
		ngHtml2JsPreprocessor: {
			// setting this option will create only a single module that contains templates
			// from all the files, so you can load them all with module('foo')
			moduleName: 'servoy-components',

			cacheIdFromPath: function(filepath) {
				return filepath.replace( /.*?\/servoydefault\/(.*)/, "servoydefault/$1" ).replace( /.*?\/servoycore\/(.*)/, "servoycore/$1" );
			},
		},

        frameworks: ['jasmine'],
		client: {
			jasmine: { random: false }
		},
        browsers: ['ChromeHeadless'],//

        plugins: [
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('@chiragrupani/karma-chromium-edge-launcher'),
            require('karma-coverage'),
            require('karma-junit-reporter'),
            require('karma-ng-html2js-preprocessor'),
            require('karma-firefox-launcher')
        ],
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
