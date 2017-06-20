angular.module('servoydefaultSplitpane',['servoy']).directive('servoydefaultSplitpane', function($apifunctions, $svyProperties, $sabloConstants, $rootScope, $window,$timeout) {  
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
			if ($scope.model.divSize == undefined) $scope.model.divSize = 5;
			
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
			
			function getHandlerElement()
			{
				var splitter = $element.children(0).children(0);
				if (splitter.children().length == 3)
				{
					return $(splitter.children()[1]);
				}
				return $();
			}
			
			function initDivLocation(newValue) {
				var multiplier;
				
				if ($scope.model.divLocation === -1) {
					// default value, half of design size
					newValue = ($scope.model.tabOrientation == -2? $scope.model.size.width:$scope.model.size.height)
					multiplier = 1/2;
				}
				else if ($scope.model.divLocation > 0 && $scope.model.divLocation <= 1)
				{
					multiplier = $scope.model.divLocation;
				}	
				if (multiplier)
				{
					$scope.model.divLocation = Math.round(newValue * multiplier);
					processDivLocation();
				}	
			}
			
			function processDivLocation() {
				if(!splitPane1 || !splitPane2) return;
				var jqueryDivEl = getHandlerElement();
				if (jqueryDivEl.length == 0) {
					$timeout(processDivLocation,10);
					return;
				}
				initDivLocation($scope.model.tabOrientation == -2? $scope.api.getWidth():$scope.api.getHeight());

				var pos = $scope.model.divLocation;
				var divSize = $scope.model.divSize;
				if ((!divSize && divSize !== 0) || divSize <0) divSize = 5;
				if($scope.model.tabOrientation == -3) { 
					if(pos <= 1) {
						pos = $scope.model.size.height * pos;
					}
					jqueryDivEl.css('top', pos + 'px');
					splitPane1.css('height', pos + 'px');
					splitPane2.css('top', (pos+divSize) + 'px');
					if (divSize === 0)
					{
						splitPane2.css('border-top-width','0px');
					}
					else
					{
						splitPane2.css('border-top-width','1px');
					}	
				}
				else {
					if(pos <= 1) {
						pos = $scope.model.size.width * pos;
					}
					jqueryDivEl.css('left', pos + 'px');
					splitPane1.css('width', pos + 'px');
					splitPane2.css('left', (pos+divSize) + 'px');
					if (divSize === 0)
					{
						splitPane2.css('border-left-width','0px');
					}
					else
					{
						splitPane2.css('border-left-width','1px');
					}
				}
			}

			var previous = -1;
			function processResize() {
				var delta  = 0;
				if($scope.model.tabOrientation == -3) {
					if (previous == -1) {
						previous = $element[0].firstChild.clientHeight;
					}
					delta = $element[0].firstChild.clientHeight - previous;
					previous = $element[0].firstChild.clientHeight;
				}
				else if($scope.model.tabOrientation == -2) {
					if (previous == -1) {
						previous = $element[0].firstChild.clientWidth;
					}
					delta = $element[0].firstChild.clientWidth - previous;
					previous = $element[0].firstChild.clientWidth;
				}
				if (delta != 0)
					$scope.model.divLocation += Math.round(delta * $scope.model.resizeWeight); // the divLocation watch will do the rest
			}
			// initialize 'previous'
			processResize();

			var resizeTimeout;
			function onResize() {
				if(resizeTimeout) {
					$timeout.cancel(resizeTimeout);
				}
				resizeTimeout = $timeout(processResize, 50);
			}
			$window.addEventListener('resize',onResize);

			if($scope.model.tabOrientation == -3) {
				$scope.$watch("model.size.height", function(newValue, oldValue) {
					if (newValue !== oldValue) {
						processResize();
					}
				});
			} 
			else if($scope.model.tabOrientation == -2) {
				$scope.$watch("model.size.width", function(newValue, oldValue) {
					if (newValue !== oldValue) {
						processResize();    		
					}
				});
			}
			
			$scope.$watch("model.resizeWeight", function(newValue,oldValue) {
				if (newValue === oldValue) return;
				processResize()
			});

			$scope.$watch('model.divSize', function(newValue, oldValue){
				var dividerEl = getHandlerElement();
				if($scope.model.tabOrientation == -3) {
					dividerEl.css('height', $scope.model.divSize + 'px'); 
				} else {
					dividerEl.css('width',  $scope.model.divSize + 'px'); 
				}
				processDivLocation()
			});

			//called when the divider location is changed from server side scripting
			$scope.$watch('model.divLocation', function(newValue, oldValue){
				if ((newValue || newValue === 0) && newValue  !== oldValue) {
					processDivLocation();
					if($scope.handlers.onChangeMethodID) {
						$scope.$evalAsync(function() {
							$scope.handlers.onChangeMethodID(-1,$.Event("change"));
						});
					}
					// let the containing forms re-calculate their size
					$window.dispatchEvent(new Event('resize'));
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
				var dividerEl = getHandlerElement();
				var dividerLocation;
				if($scope.model.tabOrientation == -3) {
					dividerLocation = dividerEl.css('top'); 
				}
				else {
					dividerLocation = dividerEl.css('left'); 
				}

				return dividerLocation ? parseInt(dividerLocation.substring(0, dividerLocation.length - 2)) : 0;
			}

			$scope.$watch("model.tabs[0].containsFormId", function(newValue, oldValue) {
				if (newValue) {
					$scope.svyServoyapi.formWillShow(newValue, $scope.model.tabs[0].relationName, 0);
				}
			});
			$scope.$watch("model.tabs[1].containsFormId", function(newValue, oldValue) {
				if (newValue) {
					$scope.svyServoyapi.formWillShow(newValue, $scope.model.tabs[1].relationName, 1);
				}
			});

			$scope.$watch("model.visible", function(newValue,oldValue) {
    	  		if (newValue !== oldValue)
    	  		{
    	  			if (newValue)
    	  			{
    	  				if ($scope.model.tabs && $scope.model.tabs[0] && $scope.model.tabs[0].containsFormId) {
    	  					$scope.svyServoyapi.formWillShow($scope.model.tabs[0].containsFormId, $scope.model.tabs[0].relationName,0);
    	  				}
    	  				if ($scope.model.tabs && $scope.model.tabs[1] && $scope.model.tabs[1].containsFormId) {
    	  					$scope.svyServoyapi.formWillShow($scope.model.tabs[1].containsFormId, $scope.model.tabs[1].relationName,1);
    	  				}
    	  			}
    	  			else
    	  			{
    	  				if ($scope.model.tabs && $scope.model.tabs[0] && $scope.model.tabs[0].containsFormId) {
    	  					$scope.svyServoyapi.hideForm($scope.model.tabs[0].containsFormId);
    	  				}
    	  				if ($scope.model.tabs && $scope.model.tabs[1] && $scope.model.tabs[1].containsFormId) {
    	  					$scope.svyServoyapi.hideForm($scope.model.tabs[1].containsFormId);
    	  				}
    	  			}	
  			}	
  		  });
			
			$scope.api.getWidth = function() {
				return $scope.model.anchors ? $apifunctions.getWidth($element[0])() : $scope.model.size.width;
			}
			$scope.api.getHeight = function() {
				return $scope.model.anchors ? $apifunctions.getHeight($element[0])() : $scope.model.size.height;
			}
			$scope.api.getLocationX = function() {
				return $scope.model.anchors ? $apifunctions.getX($element[0])() : $scope.model.location.x;
			}
			$scope.api.getLocationY = function() {
				return $scope.model.anchors ? $apifunctions.getY($element[0])() : $scope.model.location.y;
			}
			
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
			for (var key in $scope.model) {
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