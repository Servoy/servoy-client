angular.module('servoydefaultCombobox', ['servoy', 'ui.select'])
.directive('servoydefaultCombobox', ['$timeout', function ($timeout) {
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			api: "=svyApi",
			handlers: "=svyHandlers",
			svyServoyapi: "="
		},
		controller: function ($scope) {
			var minHeight = $scope.model.size.height + 'px';
			$scope.style = {
					'min-height': minHeight,
					height: '100%',
					'min-width': $scope.model.size.width + 'px',
					width: '100%'
			};

			$scope.findMode = false;
		},
		link: function (scope, element, attrs) {

			scope.$watch("model.format", function (newVal) {
				if (newVal && newVal["text-transform"]) {
					scope.style["text-transform"] = newVal["text-transform"];
				}
			});

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
			scope.api.setValueListItems = function (values) {
				var valuelistItems = [];
				var i = 0;
				var item = {};
				for (i = 0; i < values.length; i++) {
					item = {};
					item.displayValue = values[i][0];
					if (values[i][1] !== undefined) {
						item.realValue = values[i][1];
					}
				}
				scope.model.valuelistID = valuelistItems;
			};
			
			/**
	    	* Request the focus to this combobox.
	    	* @example %%prefix%%%%elementName%%.requestFocus();
	    	* @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
	    	*/
			scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) { 
				var input = element.find('.ui-select-match');
				if (mustExecuteOnFocusGainedMethod === false && scope.handlers.onFocusGainedMethodID)
				{
					input.unbind('focus');
					input[0].focus();
					input.bind('focus', scope.handlers.onFocusGainedMethodID)
				}
				else
				{
					input[0].focus();
				}
			}

			var storedTooltip = false;
			scope.api.onDataChangeCallback = function(event, returnval) {
				var ngModel = element.children().controller("ngModel");
				var stringValue = (typeof returnval === 'string' || returnval instanceof String);
				if (!returnval || stringValue) {
					element[0].focus();
					ngModel.$setValidity("", false);
					if (stringValue) {
						if (storedTooltip === false) { 
							storedTooltip = scope.model.toolTipText; 
						}
						scope.model.toolTipText = returnval;
					}
				}
				else {
					ngModel.$setValidity("", true);
					if (storedTooltip !== false) scope.model.toolTipText = storedTooltip;
					storedTooltip = false;
				}
			};

			scope.onItemSelect = function (event) {
				$timeout(function () {
					if (scope.handlers.onActionMethodID) {
						scope.handlers.onActionMethodID(event);
					}
					scope.svyServoyapi.apply('dataProviderID');
				}, 0);
			};
		},
		templateUrl: 'servoydefault/combobox/combobox.html'
	};
}])
.filter('emptyOrNull', function () {
	return function (item) {
		if (item === null || item === '') {return '&nbsp;'; }
		return item;
	};
})
.filter('showDisplayValue', function () { // filter that takes the realValue as an input and returns the displayValue
	return function (input, valuelist) {
		var i = 0;
		var realValue = input;
		if (input && valuelist) {
			if (input.hasOwnProperty("realValue")) {
				realValue = input.realValue;
			}
			//TODO performance upgrade: change the valuelist to a hashmap so that this for loop is no longer needed. 
			//maybe to something like {realValue1:displayValue1, realValue2:displayValue2, ...}
			for (i = 0; i < valuelist.length; i++) {
				if (realValue === valuelist[i].realValue) {
					return valuelist[i].displayValue;
				}
			}
		}
		return input;
	};
});
