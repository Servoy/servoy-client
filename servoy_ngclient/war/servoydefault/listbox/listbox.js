angular.module('svyListbox',['servoy']).directive('svyListbox', function($parse) {  
	return {
		restrict: 'E',
		transclude: true,
		scope: {
			model: "=svyModel",
			handlers: "=svyHandlers",
			api: "=svyApi"
		},
		require: 'ngModel',
		compile: function(tElement, tAttrs) {
			var isMultiSelect = true;
			if ($parse(tAttrs.svyModel)(angular.element(tElement).scope()).multiselectListbox != true)
			{
				false;
				tElement.removeAttr("multiple");
			}
			return function($scope, $element, $attrs, ngModel) {
				$scope.style = {width:'100%',height:'100%',overflow:'hidden'}
				$scope.findMode = false;
				$scope.$watch('model.dataProviderID', function() {
					if(!$scope.model.dataProviderID)
					{
						$scope.convertModel = null;
					}
					else
					{	
						// TODO needs to be automatic
						if(isMultiSelect){
							$scope.convertModel = ($scope.model.dataProviderID+'').split('\n');	
						}else{
							$scope.convertModel = [$scope.model.dataProviderID]
						}
					}
				})
				$scope.$watch('convertModel', function() {
					var oldValue = $scope.model.dataProviderID;
					var newValue = null;
					if(!$scope.convertModel)
					{
						newValue = null;
					}
					else
					{
						if(isMultiSelect){
							newValue = $scope.convertModel.join('\n');	
						}else{
							newValue = $scope.convertModel[0];	
						}						 
					}
					if (oldValue != newValue)
					{
						$scope.model.dataProviderID = newValue;
						$scope.handlers.svy_apply('dataProviderID');
					}	  
				})

				$scope.api.setScroll = function(x, y) {
					$element.scrollLeft(x);
					$element.scrollTop(y);
				}

				$scope.api.getScrollX = function() {
					return $element.scrollLeft();
				}

				$scope.api.getScrollY = function() {
					return $element.scrollTop();
				}

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
			} 
		},
		templateUrl: 'servoydefault/listbox/listbox.html',
		replace: true
	};
})





