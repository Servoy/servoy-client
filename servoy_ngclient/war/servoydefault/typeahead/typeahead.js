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
        var hasRealValues = false;

        $scope.$watch('model.valuelistID', function() {
          if (!$scope.model.valuelistID) return; // not loaded yet
          hasRealValues = false;
          for (var i = 0; i < $scope.model.valuelistID.length; i++) {
            var item = $scope.model.valuelistID[i];
            if (item.realValue != item.displayValue) {
              hasRealValues = true;
              break;
            }
          }
        })

        $scope.doSvyApply = function() {
          if ($('[typeahead-popup]').attr('aria-hidden') == "true") {
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
        	$scope.svyServoyapi.apply('dataProviderID');
          }
        }

        /**
         * Sets the display/real values to the custom valuelist of the element (if element has custom valuelist).
         * This does not affect the value list with same name list on other elements or value lists at application level.
         * Should receive a dataset parameter, first column is for display values, second column (optional) is for real values.
         * @example
         * var dataset = databaseManager.createEmptyDataSet(0,new Array('display_values','optional_real_values'));
         * dataset.addRow(['aa',1]);
         * dataset.addRow(['bb',2]);
         * dataset.addRow(['cc',3]);
         * // %%prefix%%%%elementName%% should have a valuelist attached
         * %%prefix%%%%elementName%%.setValueListItems(dataset);
         *
         * @param value first column is display value, second column is real value
         */
        $scope.api.setValueListItems = function(values) {
          var valuelistItems = [];
          for (var i = 0; i < values.length; i++) {
            var item = {};
            item['displayValue'] = values[i][0];
            if (values[i][1] !== undefined) {
              item['realValue'] = values[i][1];
            }
            valuelistItems.push(item);
          }
          $scope.model.valuelistID = valuelistItems;
        }
        
        /**
    	* Request the focus to this typeahead.
    	* @example %%prefix%%%%elementName%%.requestFocus();
    	*/
		$scope.api.requestFocus = function() { 
			$element[0].focus()
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
		 * Selects all the contents of the typeahaead.
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
