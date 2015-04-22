angular.module('servoydefaultTypeahead', ['servoy'])
  .directive('servoydefaultTypeahead', ['formatFilterFilter','$apifunctions', function(formatFilter,$apifunctions) {
    return {
      restrict: 'E',
      require: 'ngModel',
      scope: {
        model: "=svyModel",
        svyServoyapi: "=",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      link: function($scope, $element, $attrs, ngModel) {

        $scope.style = {
          width: '100%',
          height: '100%',
          overflow: 'hidden'
        }
        
        $scope.onClick = function(event){
      	  if ($scope.model.editable == false && $scope.handlers.onActionMethodID)
      	  {
      		  $scope.handlers.onActionMethodID(event);
      	  }	
        }
        
        $scope.findMode = false;

        $scope.formatLabel = function(model) {
          var displayFormat = undefined;
          var type = undefined;
          var displayValue = model;
          if ($scope.model.valuelistID) {
            for (var i = 0; i < $scope.model.valuelistID.length; i++) {
              if (model === $scope.model.valuelistID[i].realValue) {
                displayValue = $scope.model.valuelistID[i].displayValue;
                break;
              }
            }
          }
          else {
            displayValue = model;
          }
          if ($scope.model.format && $scope.model.format.display) displayFormat = $scope.model.format.display;
          if ($scope.model.format && $scope.model.format.type) type = $scope.model.format.type;
          return formatFilter(displayValue, displayFormat, type);
        }
        $scope.onSelect = function(item,model,label){
        	if (hasRealValues && $element.val() === model)
        	{
        		// if real value was typed we have to manually set display value because formatLabel will not be called
        		$element.val(label);
        	}	
        }
        
        var hasRealValues = false;

        $scope.$watch('model.valuelistID', function() {
          if (!$scope.model.valuelistID || $scope.model.valuelistID.length == 0) return; // not loaded yet or already filtered
          hasRealValues = false;
          for (var i = 0; i < $scope.model.valuelistID.length; i++) {
            var item = $scope.model.valuelistID[i];
            if (item.realValue != item.displayValue) {
              hasRealValues = true;
              break;
            }
          }
        })

        var editing = false;
        
        $scope.startEdit = function() {
        	editing = true;
        }
        
        $scope.doSvyApply = function() {
          if (!editing) 
        	  return;
          if ($('[typeahead-popup]').attr('aria-hidden') == "true") {
        	editing = false;
            if ($scope.model.valuelistID) {
              var hasMatchingDisplayValue = false;
              for (var i = 0; i < $scope.model.valuelistID.length; i++) {
                if ($element.val() === $scope.model.valuelistID[i].displayValue) {
                  hasMatchingDisplayValue = true;
                  break;
                }
              }
              if (!hasMatchingDisplayValue && hasRealValues) {
                $scope.model.dataProviderID = null;
              }
            }
            $scope.svyServoyapi.apply('dataProviderID');
          } 
          else if (!hasRealValues)
          {
        	editing = false;
        	$scope.svyServoyapi.apply('dataProviderID');
          }
        }

        /**
    	* Request the focus to this typeahead.
    	* @example %%prefix%%%%elementName%%.requestFocus();
    	* @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
    	*/
		$scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) { 
			if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID)
			{
				$element.unbind('focus');
				$element[0].focus();
				$element.bind('focus', $scope.handlers.onFocusGainedMethodID)
			}
			else
			{
				$element[0].focus();
			}
		}
        
        /**
		 * Returns the currently selected text in the specified typeahead. 
		 * @example var my_text = %%prefix%%%%elementName%%.getSelectedText();
		 * @return {String} The selected text in the text field.
		 */
		$scope.api.getSelectedText = $apifunctions.getSelectedText($element[0]);
		/**
		 * Replaces the selected text; if no text has been selected, the replaced value will be inserted at the last cursor position.
		 * @example %%prefix%%%%elementName%%.replaceSelectedText('John');
		 * @param s The replacement text.
		 */
		$scope.api.replaceSelectedText = $apifunctions.replaceSelectedText($element[0]);
		/**
		 * Selects all the contents of the typeahead.
		 * @example %%prefix%%%%elementName%%.selectAll();
		 */
		$scope.api.selectAll = $apifunctions.selectAll($element[0]);

        var storedTooltip = false;
        $scope.api.onDataChangeCallback = function(event, returnval) {
          var stringValue = typeof returnval == 'string'
          if (!returnval || stringValue) {
            $element[0].focus();
            ngModel.$setValidity("", false);
            if (stringValue) {
              if (storedTooltip == false)
                storedTooltip = $scope.model.toolTipText;
              $scope.model.toolTipText = returnval;
            }
          } 
          else {
            ngModel.$setValidity("", true);
            if (storedTooltip !== false) $scope.model.toolTipText = storedTooltip;
            storedTooltip = false;
          }
        }
      },
      templateUrl: 'servoydefault/typeahead/typeahead.html',
      replace: true
    };
  }])
