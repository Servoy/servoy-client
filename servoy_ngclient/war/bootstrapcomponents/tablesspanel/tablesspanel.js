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
		  
    	  $scope.$watch("model.containedForm", function(newValue,oldValue) {
    	  		if (newValue !== oldValue)
    	  		{
					if (newValue) $scope.svyServoyapi.formWillShow(newValue,$scope.model.relationName);
					if (oldValue) $scope.svyServoyapi.hideForm(oldValue);
				}	
		  });
		
    	  $scope.getContainerStyle = function() {
    		  return {position:"relative", minHeight:$scope.model.height+"px"};
    	  }
    	  
    	  $scope.showEditorHint = function()
    	  {
    		  return !$scope.model.containedForm && $element[0].getAttribute("svy-id") !== null;
    	  }
    	  
      },
      templateUrl: 'bootstrapcomponents/tablesspanel/tablesspanel.html'
    };
  })
  
  
  
