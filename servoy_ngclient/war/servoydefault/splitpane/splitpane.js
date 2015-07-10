angular.module('servoydefaultSplitpane',['servoy']).directive('servoydefaultSplitpane', function($apifunctions) {  
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			svyServoyapi: "=",
			handlers: "=svyHandlers",
			api: "=svyApi"
		},
		controller: function($scope, $element, $attrs, $rootScope) {

			$scope.pane1MinSize = 30;
			$scope.pane2MinSize = 30;

			$scope.resizeWeight = 0;

			function initDivLocation(newValue) {
				if ($scope.model.divLocation === -1) {
					$scope.model.divLocation = newValue / 2;
				}
			}

			if($scope.model.tabOrientation == -3) {
				$scope.$watch("model.size.height", function(newValue, oldValue) {
					initDivLocation(newValue);
					var delta = newValue - oldValue;
					if(delta != 0) {
						$scope.model.divLocation = (getBrowserDividerLocation() - delta) + Math.round(delta * $scope.resizeWeight); // the divLocation watch will do the rest
					}
				});
			} 

			if($scope.model.tabOrientation == -2) {
				$scope.$watch("model.size.width", function(newValue, oldValue) {
					initDivLocation(newValue);
					var delta = newValue - oldValue;
					if(delta != 0) {
						$scope.model.divLocation = (getBrowserDividerLocation() - delta) + Math.round(delta * $scope.resizeWeight);  // the divLocation watch will do the rest
					}     				 
				});
			}

			$scope.$watch('model.divSize', function(newValue, oldValue){
				var dividerEl = angular.element($element[0].querySelector(".split-handler"));
				if($scope.model.tabOrientation == -3) {
					dividerEl.css('height', $scope.model.divSize + 'px'); 
				} else {
					dividerEl.css('width',  $scope.model.divSize + 'px'); 
				}
			});

			//called when the divider location is changed from server side scripting
			$scope.$watch('model.divLocation', function(newValue, oldValue){
				if (newValue && newValue  !== oldValue) {
					var dividerEl = angular.element($element[0].querySelector(".split-handler"));
					var pane1 = angular.element($element[0].querySelector(".split-pane1"));
					var pane2 = angular.element($element[0].querySelector(".split-pane2"));

					var pos =  $scope.model.divLocation;;
					if($scope.model.tabOrientation == -3) { 
						if(pos < 1) {
							pos = $scope.model.size.height * pos;
						}
						dividerEl.css('top', pos + 'px');
						pane1.css('height', pos + 'px');
						pane2.css('top', pos + 'px');
					}
					else {
						if(pos < 1) {
							pos = $scope.model.size.width * pos;
						}
						dividerEl.css('left', pos + 'px');
						pane1.css('width', pos + 'px');
						pane2.css('left', pos + 'px');
					}
					if($scope.handlers.onChangeMethodID) $scope.handlers.onChangeMethodID(-1,$.Event("change"));
				}
			});

			if ($scope.model.tabs && $scope.model.tabs[0] && $scope.model.tabs[0].containsFormId) {
				$scope.svyServoyapi.formWillShow($scope.model.tabs[0].containsFormId, $scope.model.tabs[0].relationName,0);
			};
			if ($scope.model.tabs && $scope.model.tabs[1] && $scope.model.tabs[1].containsFormId) {
				$scope.svyServoyapi.formWillShow($scope.model.tabs[1].containsFormId, $scope.model.tabs[1].relationName,1);
			};
			//called by bg-splitter when the user changes the divider location with the mouse
			$scope.onChange = function() {
				$scope.model.divLocation = getBrowserDividerLocation();
				$scope.$apply(); // not in angular so we need a digest that will trigger the watch that will then trigger the handler
			}

			$scope.getForm = function(tab) {
				if (!tab) return null;
				return $scope.svyServoyapi.getFormUrl(tab.containsFormId);
			}

			function getBrowserDividerLocation() {
				var dividerEl = angular.element($element[0].querySelector(".split-handler"));
				var dividerLocation;
				if($scope.model.tabOrientation == -3) {
					dividerLocation = dividerEl.css('top'); 
				}
				else {
					dividerLocation = dividerEl.css('left'); 
				}

				return dividerLocation ? dividerLocation.substring(0, dividerLocation.length - 2) : 0;
			}

			$scope.$watch("model.tabs[0].containsFormId", function(newValue, oldValue) {
				if (newValue) {
					$scope.svyServoyapi.formWillShow(newValue, $scope.model.tabs[0].relationName, 0);
				}
			});
			$scope.$watch("model.tabs[1].containsFormId", function(newValue, oldValue) {
				if (newValue) {
					$scope.svyServoyapi.formWillShow(newValue, $scope.model.tabs[1].relationName, 0);
				}
			});

			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);
		},
		templateUrl: 'servoydefault/splitpane/splitpane.html'
	};
})