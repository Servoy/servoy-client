angular.module('servoydefaultListbox',['servoy'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("servoydefault/listbox/listbox.html").then(function(result){
		$templateCache.put("servoydefault/listbox/listbox.html", result.data);
    });
	$http.get("servoydefault/listbox/listbox_multiple.html").then(function(result){
		$templateCache.put("servoydefault/listbox/listbox_multiple.html", result.data);
    });	
}]).directive('servoydefaultListbox', ['$parse','$templateCache','$compile',function($parse,$templateCache,$compile) {  
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			handlers: "=svyHandlers",
			api: "=svyApi",
			svyServoyapi: "="
		},
		link: function($scope, $element, $attrs) {
				var isMultiSelect = $scope.model.multiselectListbox;
				$element.html($templateCache.get(isMultiSelect ? "servoydefault/listbox/listbox_multiple.html" : "servoydefault/listbox/listbox.html"));
		        $compile($element.contents())($scope);
		        
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
							$scope.convertModel = $scope.model.dataProviderID
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
							newValue = $scope.convertModel;	
						}						 
					}
					if (oldValue != newValue)
					{
						$scope.model.dataProviderID = newValue;
						$scope.svyServoyapi.apply('dataProviderID');
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
						$scope.model.editable = $scope.wasEditable != undefined ? $scope.wasEditable : editable;
					}
				}; 
		},
		replace: true
	};
}])





