angular.module('servoydefaultRadio', [ 'servoy' ]).directive('servoydefaultRadio', function($apifunctions, $svyProperties, $sabloConstants) {
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
			$scope.style = {
				width : '100%',
				height : '100%'
			}

			$scope.radioClicked = function() {
				$scope.model.dataProviderID = $scope.model.valuelistID[0].realValue;
				$scope.svyServoyapi.apply('dataProviderID')
			}

			/**
			 * Request the focus to this radio button.
			 * @example %%prefix%%%%elementName%%.requestFocus();
			 * @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
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

			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);

			var tooltipState = null;
			var className = null;
			var element = $element.children().first();
			var intputElement = element.children().first();
			var spanElement = element.children().last();
			Object.defineProperty($scope.model, $sabloConstants.modelChangeNotifier, {
				configurable : true,
				value : function(property, value) {
					switch (property) {
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
					case "styleClass":
						if (className)
							element.removeClass(className);
						className = value;
						if (className)
							element.addClass(className);
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
						if (value)
							intputElement.removeAttr("disabled");
						else
							intputElement.attr("disabled", "disabled");
						break;
					case "editable":
						if (value)
							intputElement.removeAttr("readonly");
						else
							intputElement.attr("readonly", "readonly");
						break;
					case "margin":
						if (value)
							intputElement.css(value);
						break;
					case "fontType":
						$svyProperties.setCssProperty(spanElement,"font",value);
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
		templateUrl : 'servoydefault/radio/radio.html'
	};
})