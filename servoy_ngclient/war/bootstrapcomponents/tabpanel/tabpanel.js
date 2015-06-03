angular.module('bootstrapcomponentsTabpanel',['servoy']).directive('bootstrapcomponentsTabpanel', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	svyServoyapi: "=",
       	handlers: "=svyHandlers",
       	api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	  
    	  $scope.getForm = function(tab) {
    		  if (tab && tab.active && tab.containedForm) { 
    			  return $scope.svyServoyapi.getFormUrl(tab.containedForm);
    		  }
    		  return "";
    	  }
    	  
    	  $scope.select = function(tab) {
    		  if (tab && tab.containedForm)
    		  {
    			  $scope.svyServoyapi.formWillShow(tab.containedForm, tab.relationName);
    			  tab.active = true;
    		  }	  
    	  }
    	  if ($scope.model.tabs && $scope.model.tabs.length >0)
    	  {
    		  $scope.model.tabs[0].active = true;
    		  $scope.svyServoyapi.formWillShow($scope.model.tabs[0].containedForm, $scope.model.tabs[0].relationName); 
    	  }
    	  
    	  $scope.getContainerHeight = function() {
    		  return {minHeight:$scope.model.height+"px"};
    	  }
    	  
      },
      templateUrl: 'bootstrapcomponents/tabpanel/tabpanel.html'
    };
  })
  
  
  
