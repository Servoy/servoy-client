angular.module('bootstrapcomponentsTextbox',['servoy']).directive('bootstrapcomponentsTextbox', function($formatterUtils) {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
    	  
    	  var formatState = null;
    	  var child = $element.children();
    	  var ngModel = child.controller("ngModel");
			
    	  $scope.$watch('model.format', function(){
    		  if ($scope.model.format)
    		  {
    			  if (formatState)
  					formatState(value);
    			  else formatState = $formatterUtils.createFormatState($element, $scope, ngModel,true,$scope.model.format);
    		  }	  
    	  })
      },
      templateUrl: 'bootstrapcomponents/textbox/textbox.html'
    };
  })
  
  
  
