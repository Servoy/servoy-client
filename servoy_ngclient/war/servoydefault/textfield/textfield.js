angular.module('svyTextfield',['servoy']).directive('svyTextfield', function($apifunctions) {  
	return {
		restrict: 'E',
		transclude: true,
		scope: {
			model: "=svyModel",
			api: "=svyApi",
			handlers: "=svyHandlers"
		},
		controller: function($scope, $element, $attrs, $log) {
			$scope.findMode = false;
			$scope.style = {width:'100%',height:'100%',overflow:'hidden'}

			// fill in the api defined in the spec file
			$scope.api.onDataChangeCallback = function(event, returnval) {
				if(!returnval) {
					$element[0].focus();
				}
			}
			$scope.api.requestFocus = function() { 
				$element[0].focus()
			}

			$scope.api.getSelectedText = $apifunctions.getSelectedText($element[0]);
			$scope.api.setSelection = $apifunctions.setSelection($element[0]);
			$scope.api.replaceSelectedText = $apifunctions.replaceSelectedText($element[0]);
			$scope.api.selectAll = $apifunctions.selectAll($element[0]);

			// special method that servoy calls when this component goes into find mode.
			$scope.api.setFindMode = function(findMode, editable) {
				$scope.findMode = findMode;
				if (findMode)
				{
					$scope.wasEditable = $scope.model.editable;
					if (!$scope.model.editable) $scope.model.editable = editable;
				}
				else
				{
					$scope.model.editable = $scope.wasEditable;
				}
			};

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





