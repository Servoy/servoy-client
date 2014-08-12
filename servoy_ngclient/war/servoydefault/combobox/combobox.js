angular.module('svyCombobox',['servoy'])

.directive('svyCombobox', ['$svyNGEvents', function($svyNGEvents) {
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        api:"=svyApi",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	   $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	   $scope.findMode = false;
    	   $scope.customClasses = "";
    	   // uncomment the following comment to use select2 as default; or the other way around
    	   $scope.isSelect2 = ($scope.model.styleClass && ($scope.model.styleClass.indexOf('select2', 0) == 0)) /**/|| (typeof $scope.model.styleClass == 'undefined')/**/;
      },
      link: function(scope, element, attr) {
    	  // see http://ivaynberg.github.io/select2/ for what this component allows (also can do typeahead, multi-edit field and so on)
    	  // we could somehow give to select2() method 'containerCssClass' and 'dropdownCssClass' as well if needed in the future (for more custom styling)
    	  
    	  var ngModel = element.children().controller("ngModel");
    	  var select2Css = null;
    	  if (scope.model.styleClass && scope.model.styleClass.indexOf('select2 ', 0) == 0) {
    		  // transform it into a select2 bootstrap combo and append styles
    		  select2Css = "select2-container-svy-xs " + scope.model.styleClass.substr(8, scope.model.styleClass.length - 8);
    	  } else if (scope.isSelect2) {
    		  // transform it into a default select2 bootstrap combo
    		  select2Css = "select2-container-svy-xs";
    	  }
    	  
    	  if (scope.handlers.onFocusGainedMethodID) {
     		   element.on("select2-focus", function(event) {
	              scope.$apply(function() {
	                scope.handlers.onFocusGainedMethodID(event);
	              });
	            });
    	  }
    	  if (scope.handlers.onFocusLostMethodID) {
     		   element.on("select2-blur", function(event) {
	              scope.$apply(function() {
	                scope.handlers.onFocusLostMethodID(event);
	              });
	            });
    	  }
    	  
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
        		  valuelistItems.push(item); 
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
			var stringValue = typeof returnval == 'string'
			if(!returnval || stringValue) {
				element[0].focus();
				// TODO this will set ng-invalid on the tag, but "Select2" added many internal things to it that have 
				// there own background color and img, so the color of ng-invalid won't be shown here...
				// even setting ng-invalid class on those doesn't have to work because the background settings that is in select2.css could still override it.
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
    	  
    	  if (select2Css != null) {
    		  $svyNGEvents.afterNGProcessedDOM(function () {
    			  // $evalAsync so that the dom is processed already by angular; for example if there's a "<label for=.." linked to this combobox,
    			  // the select's "id" attr must already be replaced by angular before we call select2
	    		  $(element).children("select").select2({
	    			  minimumResultsForSearch: -1, // don't show the search input when there are few items in combobox to choose from
	    			  containerCss: scope.style,
	    			  containerCssClass: select2Css
	    		  });
	    		  
		    	  element.on('$destroy', function() {
		    		  $(element).children("select").select2("destroy");
		    	  });
    		  }, true);
    	  } else scope.customClasses = (scope.model.styleClass ? scope.model.styleClass : "form-control input-sm svy-padding-xs");
      },
      templateUrl: 'servoydefault/combobox/combobox.html',
      replace: true
    };
}]);
