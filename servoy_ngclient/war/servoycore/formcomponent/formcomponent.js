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
        	   $scope.$watch("model.containedForm.svy_form_url", function(newValue) { 
        		   if (newValue) {
					$http.get(newValue, {cache: $templateCache}).then(function(result) {
					  	$element.html(result.data);
					  	$scope.layout = {}
					  	for(var x in $scope.model) {
					  		// for now just create for all model properties
					  		$scope.api[x] = {};
					  		$scope.layout[x] = { position: 'absolute' }
					  		$servoyInternal.applyBeanData($scope.model[x], $scope.layout[x] , $scope.model[x], null, null, null, null, null)
					  	}
					    $compile($element.contents())($scope);
					});
        		   }
        	   });
        }
    }
}])