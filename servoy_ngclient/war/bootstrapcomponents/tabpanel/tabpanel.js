angular.module('bootstrapcomponentsTabpanel',['servoy']).directive('bootstrapcomponentsTabpanel', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	svyServoyapi: "=",
       	handlers: "=svyHandlers",
       	api: "=svyApi"
      },
      controller: function($scope, $element, $attrs, webStorage) {
    	  
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
    			  if ($scope.model.tabs[$scope.model.tabIndex-1] == tab) {
    				  $scope.svyServoyapi.formWillShow(tab.containedForm, tab.relationName);
    			  }
    			  else {
					var promise =  $scope.svyServoyapi.hideForm($scope.model.tabs[$scope.model.tabIndex-1].containedForm);
					promise.then(function(ok) {
					  $scope.model.tabIndex = getTabIndex(tab)+1;
    			 	  $scope.svyServoyapi.formWillShow(tab.containedForm, tab.relationName);
					})  
    			  }
    		  }	  
    	  }
    	  if ($scope.model.tabs && $scope.model.tabs.length >0)
    	  {
    		  var index = 1;
    		  if ($scope.$parent && $scope.$parent.formname)
    		  {
    			  var key = $scope.$parent.formname +"_" + $element.attr('name')+"_tabindex";
    			  var storageValue= webStorage.session.get(key);
    			  if (storageValue)
    			  {
    				  index = parseInt(storageValue);
    				  if (index > $scope.model.tabs.length)
    				  {
    					  index = 1;
    				  }	  
    			  }	  
    		  }
    		  if ($scope.model.tabs[index-1].containedForm)
    		  {
    			  $scope.model.tabIndex = index;
        		  $scope.model.tabs[index-1].active = true;
        		  $scope.svyServoyapi.formWillShow($scope.model.tabs[index-1].containedForm, $scope.model.tabs[index-1].relationName);  
    		  }	  
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
						$scope.svyServoyapi.formWillShow($scope.model.tabs[newValue-1].containedForm,$scope.model.tabs[newValue-1].relationName);
						$scope.model.tabs[newValue-1].active = true;
					}
					if ($scope.$parent && $scope.$parent.formname)
					{
						 var key = $scope.$parent.formname +"_" + $element.attr('name')+"_tabindex";
						 webStorage.session.add(key,newValue);
					}
				}	
		  });
		  
		   $scope.$watch("model.tabs", function(newValue,oldValue) {
    	  		if (newValue != oldValue)
    	  		{
    	  			var oldForm = oldValue && oldValue.length > 0?  oldValue[$scope.model.tabIndex-1].containedForm : null;
    	  			var newTabIndex = $scope.model.tabIndex;
    	  			if (!newValue || newValue.length == 0)
    	  			{
    	  				newTabIndex = 0;
    	  			}
    	  			else if (newValue.length < newTabIndex)
    	  			{
    	  				newTabIndex = newValue.length -1;
    	  			}
    	  			else if (newValue && newValue.length > 0 && !newTabIndex)
    	  			{
    	  				newTabIndex = 1;
    	  			}
    	  			var newForm = newValue && newValue.length > 0 ?  newValue[newTabIndex-1].containedForm : null;
    	  			if (newForm != oldForm)
    	  			{
	    	  			if (oldForm) $scope.svyServoyapi.hideForm(oldForm);
						if (newForm) $scope.svyServoyapi.formWillShow(newForm, newValue[newTabIndex-1].relationName);
    	  			}
    	  			if (newTabIndex != $scope.model.tabIndex)
    	  			{
    	  				$scope.model.tabIndex = newTabIndex;
    	  			}
				}	
		  });
		  
    	  $scope.getContainerStyle = function() {
    		  return {position:"relative", minHeight:$scope.model.height+"px"};
    	  }
    	  
    	  $scope.showEditorHint = function()
    	  {
    		  return (!$scope.model.tabs || $scope.model.tabs.length == 0) && $element[0].getAttribute("svy-id") !== null;
    	  }
    	  
      },
      templateUrl: 'bootstrapcomponents/tabpanel/tabpanel.html'
    };
  })
  
  
  
