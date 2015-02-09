angular.module('bootstrapcomponentsTablesspanel',['servoy']).directive('bootstrapcomponentsTablesspanel', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	svyServoyapi: "=",
       	handlers: "=svyHandlers",
       	api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	  
    	  $scope.getActiveTabUrl = function() {
    		  if ($scope.model.containedForm)
    		  {
    			  return $scope.svyServoyapi.getFormUrl($scope.model.containedForm)
    		  }  
    		  return "";
    	  }
    	  
      },
      templateUrl: 'bootstrapcomponents/tablesspanel/tablesspanel.html'
    };
  })
  
  
  
