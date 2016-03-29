angular.module('bootstrapcomponentsTablesspanel',['servoy']).directive('bootstrapcomponentsTablesspanel', function($sabloApplication) {  
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
    		  var height = 0;
    		  if ($scope.model.height)
    		  {
    			  height = $scope.model.height
    		  }
    		  else if ($scope.model.containedForm && $sabloApplication.hasFormStateWithData($scope.model.containedForm))
    		  {
    			  // for absolute form default height is design height, for responsive form default height is 0
    			  var formState = $sabloApplication.getFormStateEvenIfNotYetResolved($scope.model.containedForm);
    			  if (formState && formState.properties && formState.properties.absoluteLayout)
    			  {
    				  height = formState.properties.designSize.height; 
    			  }	  
    		  }	  
    		  return {position:"relative", minHeight:height+"px"};
    	  }
    	  
    	  $scope.showEditorHint = function()
    	  {
    		  return !$scope.model.containedForm && $element[0].getAttribute("svy-id") !== null;
    	  }
    	  
      },
      templateUrl: 'bootstrapcomponents/tablesspanel/tablesspanel.html'
    };
  })
  
  
  
