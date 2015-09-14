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
    	  
    	  if ($scope.model.containedForm)
		  {
    		  $scope.svyServoyapi.formWillShow($scope.model.containedForm,$scope.model.relationName);
		  }
    	  
    	  $scope.getContainerStyle = function() {
    		  return {position:"relative", minHeight:$scope.model.height+"px"};
    	  }
      },
      templateUrl: 'bootstrapcomponents/tablesspanel/tablesspanel.html'
    };
  })
  
  
  
