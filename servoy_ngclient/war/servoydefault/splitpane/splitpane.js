angular.module('servoydefaultSplitpane',['servoy']).directive('servoydefaultSplitpane', function($apifunctions, $svyProperties, $sabloConstants, $rootScope, $window) {  
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			svyServoyapi: "=",
			handlers: "=svyHandlers",
			api: "=svyApi"
		},
		controller: function($scope, $element, $attrs) {

			if ($scope.model.resizeWeight == undefined) $scope.model.resizeWeight = 0;
			if ($scope.model.pane1MinSize == undefined) $scope.model.pane1MinSize = 30;
			if ($scope.model.pane2MinSize == undefined) $scope.model.pane2MinSize = 30;
			
			var splitPane1;
			var splitPane2;
			$scope.registerSplitPane = function(splitPaneElement, which) {
				if (which == "split1") {
					splitPane1 = splitPaneElement;
				}
				else if (which == "split2") {
					splitPane2 = splitPaneElement;
				}
				processDivLocation();
			} 
			
			function processDivLocation() {
				if(!splitPane1 || !splitPane2) return;
				var dividerEl = angular.element($element[0].querySelector(".split-handler"));

				var pos = $scope.model.divLocation;
				if($scope.model.tabOrientation == -3) { 
					if(pos < 1) {
						pos = $scope.model.size.height * pos;
					}
					dividerEl.css('top', pos + 'px');
					splitPane1.css('height', pos + 'px');
					splitPane2.css('top', pos + 'px');
				}
				else {
					if(pos < 1) {
						pos = $scope.model.size.width * pos;
					}
					dividerEl.css('left', pos + 'px');
					splitPane1.css('width', pos + 'px');
					splitPane2.css('left', pos + 'px');
				}
			}

			function initDivLocation(newValue) {
				if ($scope.model.divLocation === -1) {
					$scope.model.divLocation = Math.round(newValue / 2);
					processDivLocation();
				}
			}
			
			function processResize() {
				var delta  = 0;
				var standard = 0;
				if($scope.model.tabOrientation == -3) {
					delta = $element[0].firstChild.clientHeight - height;
					standard = height;
				}
				else if($scope.model.tabOrientation == -2) {
					delta = $element[0].firstChild.clientWidth - width;
					standard = width;
				}
				if (delta != 0)
					$scope.model.divLocation = standard/2 + Math.round(delta * $scope.model.resizeWeight); // the divLocation watch will do the rest
			}

			var width = $scope.model.size.width;
			var height = $scope.model.size.height;
			$window.addEventListener('resize',processResize);

			if($scope.model.tabOrientation == -3) {
				$scope.$watch("model.size.height", function(newValue, oldValue) {
					height = newValue;
					initDivLocation(newValue);
					processResize();
				});
			} 
			else if($scope.model.tabOrientation == -2) {
				$scope.$watch("model.size.width", function(newValue, oldValue) {
					width = newValue;
					initDivLocation(newValue);
					processResize();    				 
				});
			}
			
			$scope.$watch("model.resizeWeight", function(newValue,oldValue) {
				if (newValue === oldValue) return;
				processResize()
			});

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
					processDivLocation();
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
			
			var className = null;
			var element = $element.children().first();
			
			Object.defineProperty($scope.model, $sabloConstants.modelChangeNotifier, {
				configurable : true,
				value : function(property, value) {
					switch (property) {
					case "borderType":
						$svyProperties.setBorder(element, value);
						break;
					case "fontType":
						$svyProperties.setCssProperty(element,"font",value);
						break;
					case "styleClass":
						if (className)
							element.removeClass(className);
						className = value;
						if (className)
							element.addClass(className);
						break;
					}
				}
			});
			var destroyListenerUnreg = $scope.$on("$destroy", function() {
				destroyListenerUnreg();
				delete $scope.model[$sabloConstants.modelChangeNotifier];
			});
			// data can already be here, if so call the modelChange function so that it is initialized correctly.
			var modelChangFunction = $scope.model[$sabloConstants.modelChangeNotifier];
			for (key in $scope.model) {
				modelChangFunction(key, $scope.model[key]);
			}
		},
		templateUrl: 'servoydefault/splitpane/splitpane.html'
	};
}).directive("tabloadchecker",function($parse) {
	return {
		restrict: 'A',
		link: function($scope, $element, $attrs) {
			$scope.registerSplitPane($element, $attrs.tabloadchecker)
		}
	}
})