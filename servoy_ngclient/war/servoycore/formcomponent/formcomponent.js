angular.module('servoycoreFormcomponent',['servoy']).directive('servoycoreFormcomponent', ['$http','$templateCache','$compile','$servoyInternal',function($http,$templateCache,$compile,$servoyInternal) {
    return {
           restrict : 'E',
           scope : {
        	   model: '=svyModel',
        	   api : "=svyApi",
        	   svyServoyapi: "=",
        	   handlers: "=svyHandlers"
           },
           controller: function($scope, $element, $attrs)
           {
        	   $scope.$watch("model.containedForm", function(newValue) { 
        		   if (newValue) {
					  	$element.html(newValue);
					  	var formScope = $scope.svyServoyapi.getFormState();
					    $compile($element.contents())(formScope);
        		   }
        		   else {
        			   $element.html("<div></div>");
        		   }
        	   });
        }
    }
}])