angular.module('servoycoreFormcomponent',['servoy']).directive('servoycoreFormcomponent', [function() {
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
        		   $element.empty();
        		   if (newValue) {
        			   var elements = $scope.svyServoyapi.getFormComponentElements("containedForm", newValue);
					   $element.append(elements);
        		   }
        		   else {
        			   $element.html("<div>FormComponent, select a form</div>");
        		   }
        	   });
        }
    }
}])