angular.module('servoydefaultTextfield',['servoy']).directive('servoydefaultTextfield', function($apifunctions) {  
	return {
		restrict: 'E',
		require: 'ngModel',
		scope: {
			model: "=svyModel",
			api: "=svyApi",
			handlers: "=svyHandlers"
		},
		link:function($scope, $element, $attrs, ngModel) {
			$scope.findMode = false;
			$scope.style = {width:'100%',height:'100%',overflow:'hidden'}

			var storedTooltip = false;
			// fill in the api defined in the spec file

			$scope.$watch($attrs['svyFormat'], function(newVal, oldVal){
				if (newVal && newVal["text-transform"])
					$scope.style["text-transform"] = newVal["text-transform"];
			});

			$scope.api.onDataChangeCallback = function(event, returnval) {
				var stringValue = typeof returnval == 'string'
					if(!returnval || stringValue) {
						$element[0].focus();
						ngModel.$setValidity("", false);
						if (stringValue) {
							if ( storedTooltip == false)
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
			/**
	    	* Request the focus to this textaarea.
	    	* @example %%prefix%%%%elementName%%.requestFocus();
	    	*/
			$scope.api.requestFocus = function() { 
				$element[0].focus()
			}

			/**
			 * Returns the currently selected text in the specified text field. 
			 * @example var my_text = %%prefix%%%%elementName%%.getSelectedText();
			 * @return {String} The selected text in the text field.
			 */
			$scope.api.getSelectedText = $apifunctions.getSelectedText($element[0]);
			$scope.api.setSelection = $apifunctions.setSelection($element[0]);
			/**
			 * Replaces the selected text; if no text has been selected, the replaced value will be inserted at the last cursor position.
			 * @example %%prefix%%%%elementName%%.replaceSelectedText('John');
			 * @param s The replacement text.
			 */
			$scope.api.replaceSelectedText = $apifunctions.replaceSelectedText($element[0]);
			/**
			 * Selects all the contents of the text field.
			 * @example %%prefix%%%%elementName%%.selectAll();
			 */
			$scope.api.selectAll = $apifunctions.selectAll($element[0]);

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
			$scope.api.setValueListItems = function(values) 
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
				$scope.model.valuelistID = valuelistItems;
			}
		},
		templateUrl: 'servoydefault/textfield/textfield.html',
		replace: true
	};
})





