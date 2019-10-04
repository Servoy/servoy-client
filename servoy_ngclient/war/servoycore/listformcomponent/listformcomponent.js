angular.module('servoycoreListformcomponent', ['servoy']).directive('servoycoreListformcomponent', ['$compile', '$sabloConstants', function($compile, $sabloConstants) {
		return {
			restrict: 'E',
			scope: {
				model: '=svyModel',
				api: "=svyApi",
				svyServoyapi: "=",
				handlers: "=svyHandlers"
			},
			templateUrl: 'servoycore/listformcomponent/listformcomponent.html',
			controller: function($scope, $element, $attrs) {
			},
			link: function($scope, $element, $attrs) {
			}
		}
	}])