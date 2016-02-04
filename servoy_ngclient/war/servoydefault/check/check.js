angular.module('servoydefaultCheck', [ 'servoy' ]).directive('servoydefaultCheck', function($apifunctions, $svyProperties, $formatterUtils, $sabloConstants,$timeout) {
	return {
		restrict : 'E',
		scope : {
			name : "=",
			model : "=svyModel",
			handlers : "=svyHandlers",
			api : "=svyApi",
			svyServoyapi : "="
		},
		link : function($scope, $element, $attrs) {
			
			$scope.selection = false;

			$scope.$watch('model.dataProviderID', function() {
				$scope.selection = getSelectionFromDataprovider();
			})

			$scope.checkBoxClicked = function(event) {
				if ($scope.model.valuelistID && $scope.model.valuelistID[0]) {
					$scope.model.dataProviderID = $scope.model.dataProviderID == $scope.model.valuelistID[0].realValue ? null : $scope.model.valuelistID[0].realValue;
				} else if (angular.isString($scope.model.dataProviderID)) {
					$scope.model.dataProviderID = $scope.model.dataProviderID == "1" ? "0" : "1";
				} else {
					$scope.model.dataProviderID = $scope.model.dataProviderID > 0 ? 0 : 1;
				}
				$timeout(function(){
					$scope.svyServoyapi.apply('dataProviderID')
					if ($scope.handlers.onActionMethodID)
					{
						$scope.handlers.onActionMethodID(event)
					}
				},0)
			}

			/**
			 * Request the focus to this checkbox.
			 * 
			 * @example %%prefix%%%%elementName%%.requestFocus();
			 * @param mustExecuteOnFocusGainedMethod
			 *            (optional) if false will not execute the onFocusGained
			 *            method; the default value is true
			 */
			$scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) {
				var input = $element.find('input');
				if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID) {
					input.unbind('focus');
					input[0].focus();
					input.bind('focus', $scope.handlers.onFocusGainedMethodID)
				} else {
					input[0].focus();
				}
			}

			function getSelectionFromDataprovider() {
				if (!$scope.model.dataProviderID)
					return false;
				if ($scope.model.valuelistID && $scope.model.valuelistID[0]) {
					return $scope.model.dataProviderID == $scope.model.valuelistID[0].realValue;
				} else if (angular.isString($scope.model.dataProviderID)) {
					return $scope.model.dataProviderID == "1";
				} else {
					return $scope.model.dataProviderID > 0;
				}
			}

			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);

			var element = $element.children().first();
			var inputElement = element.children().first();
			var spanElement = element.children().last();
			var tooltipState = null;
			var className = null;
			Object.defineProperty($scope.model, $sabloConstants.modelChangeNotifier, {
				configurable : true,
				value : function(property, value) {
					switch (property) {
					case "styleClass":
						if (className)
							element.removeClass(className);
						className = value;
						if (className)
							element.addClass(className);
						break;
					case "borderType":
						$svyProperties.setBorder(element, value);
						break;
					case "background":
					case "transparent":
						$svyProperties.setCssProperty(element, "backgroundColor", $scope.model.transparent ? "transparent" : $scope.model.background);
						break;
					case "foreground":
						$svyProperties.setCssProperty(element, "color", value);
						break;
					case "horizontalAlignment":
						$svyProperties.setHorizontalAlignment(element, value);
						break;
					case "toolTipText":
						if (tooltipState)
							tooltipState(value);
						else
							tooltipState = $svyProperties.createTooltipState(element, value);
						break;
					case "enabled":
						if (value && $scope.model.editable)
							inputElement.removeAttr("disabled");
						else
							inputElement.attr("disabled", "disabled");
						break;
					case "editable":
						if (value  && $scope.model.enabled)
							inputElement.removeAttr("disabled");
						else
							inputElement.attr("disabled", "disabled");
						break;
					case "margin":
						if (value)
							inputElement.css(value);
						break;
					case "fontType":
						$svyProperties.setCssProperty(spanElement, "font", value);
						break;
					}
				}
			});
			var destroyListenerUnreg = $scope.$on("$destroy", function() {
				destroyListenerUnreg();
				delete $scope.model[$sabloConstants.modelChangeNotifier];
			});
			// data can already be here, if so call the modelChange function so
			// that it is initialized correctly.
			var modelChangFunction = $scope.model[$sabloConstants.modelChangeNotifier];
			for (var key in $scope.model) {
				modelChangFunction(key, $scope.model[key]);
			}

		},
		templateUrl : 'servoydefault/check/check.html'
	};
})