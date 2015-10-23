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
    	  
    	  var getTabIndex = function(tab)
    	  {
    	      if ($scope.model.tabs && tab)
    	      {
    	      	  for (var i=0;i<$scope.model.tabs.length;i++)
    	      	  {
    	      	  		if ($scope.model.tabs[i] == tab)
    	      	  		{
    	      	  			return i;
    	      	  		}
    	      	  }
    	      }
    	      return -1;
    	  }
    	  $scope.getForm = function(tab) {
    		  if (tab && tab.active && tab.containedForm) { 
    			  return $scope.svyServoyapi.getFormUrl(tab.containedForm);
    		  }
    		  return "";
    	  }
    	  
    	  $scope.select = function(tab) {
    		  if (tab && tab.containedForm)
    		  {
					var promise =  $scope.svyServoyapi.hideForm($scope.model.tabs[$scope.model.tabIndex-1]);
					promise.then(function(ok) {
					  $scope.model.tabIndex = getTabIndex(tab);
    			 	  $scope.svyServoyapi.formWillShow(tab.containedForm, tab.relationName);
    			      tab.active = true;
					})    		  
    		  }	  
    	  }
    	  if ($scope.model.tabs && $scope.model.tabs.length >0)
    	  {
    	   	  $scope.model.tabIndex = 1;
    		  $scope.model.tabs[0].active = true;
    		  $scope.svyServoyapi.formWillShow($scope.model.tabs[0].containedForm, $scope.model.tabs[0].relationName); 
    	  }
    	  
    	  $scope.$watch("model.tabIndex", function(newValue,oldValue) {
    	  		if (newValue !== oldValue)
    	  		{
    	  			if (oldValue)
    	  			{ 
    	  				$scope.svyServoyapi.hideForm($scope.model.tabs[oldValue-1].containedForm);
    	  				$scope.model.tabs[oldValue-1].active = false;
    	  			}
					if (newValue)
					{ 
						$scope.svyServoyapi.formWillShow($scope.model.tabs[newValue-1].containedForm,$scope.model.relationName);
						$scope.model.tabs[newValue-1].active = false;
					}
				}	
		  });
		  
		   $scope.$watch("model.tabs", function(newValue,oldValue) {
    	  		if (newValue != oldValue && $scope.model.tabIndex)
    	  		{
					$scope.svyServoyapi.hideForm(oldValue[$scope.model.tabIndex-1]);
					$scope.svyServoyapi.formWillShow(newValue[$scope.model.tabIndex-1].containedForm, newValue[$scope.model.tabIndex-1].relationName);
				}	
		  });
		  
    	  $scope.getContainerHeight = function() {
    		  return {minHeight:$scope.model.height+"px"};
    	  }
    	  
      },
      templateUrl: 'bootstrapcomponents/tabpanel/tabpanel.html'
    };
  })
  
  
  
