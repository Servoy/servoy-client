angular.module('servoydefaultCombobox',['servoy','ui.select'])

.directive('servoydefaultCombobox', ['$timeout', function($timeout) {
    return {
      restrict: 'E',
      scope: {
        model: "=svyModel",
        api:"=svyApi",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	   $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	   $scope.findMode = false;
      },
      link: function(scope, element, attr) {
    	  
    	  scope.api.setValueListItems = function(values) 
          {
        	  var valuelistItems = [];
        	  for (var i = 0; i < values.length; i++)
        	  {
        		  var item = {};
        		  item['displayValue'] = values[i][0];
        		  if (values[i][1] !== undefined)
        		  {
        			  item['realValue'] = values[i][1];
        		  }
        	  }
        	  scope.model.valuelistID = valuelistItems;
          }
    	  
    	// special method that servoy calls when this component goes into find mode.
     	 scope.api.setFindMode = function(findMode, editable) {
     		 scope.findMode = findMode;
     		 if (findMode)
      	 	{
      	 		scope.wasEditable = scope.model.editable;
      	 		if (!scope.model.editable) scope.model.editable = editable;
      	 	}
      	 	else
      	 	{
      	 		scope.model.editable = scope.wasEditable;
      	 	}
     	 };
     	 var storedTooltip = false;
     	scope.api.onDataChangeCallback = function(event, returnval) {
     		var ngModel = element.children().controller("ngModel");
			var stringValue = typeof returnval == 'string'
			if(!returnval || stringValue) {
				element[0].focus();
				ngModel.$setValidity("", false);
				if (stringValue) {
					if ( storedTooltip == false)
						storedTooltip = scope.model.toolTipText;
					scope.model.toolTipText = returnval;
				}
			}
			else {
				ngModel.$setValidity("", true);
				scope.model.toolTipText = storedTooltip;
				storedTooltip = false;
			}
		}
     	
     	scope.onItemSelect = function(event)
     	{
     		$timeout(function() {
     			if (scope.handlers.onActionMethodID)
         		{
         			scope.handlers.onActionMethodID(event);
         		}
         		scope.handlers.svy_apply('dataProviderID');
     		},0);
     	}
     	
      },
      templateUrl: 'servoydefault/combobox/combobox.html'
    };
}]);
