angular.module('servoydefaultButton', [ 'servoy' ]).directive('servoydefaultButton', function(formatFilterFilter, $apifunctions, $svyProperties, $sabloConstants) {
	return {
		restrict : 'E',
		scope : {
			model : "=svyModel",
			handlers : "=svyHandlers",
			api: "=svyApi",
			servoyApi: "=svyServoyapi"
		},
		controller : function($scope, $element, $attrs) {
			/**
			 * Set the focus to this button.
			 * 
			 * @example %%prefix%%%%elementName%%.requestFocus();
			 */
			$scope.api.requestFocus = function() {
				$element.find('button')[0].focus();
			}

			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);
			var tooltipState = null;
			var className;
			var element = $element.children().first();
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
					case "fontType":
						$svyProperties.setCssProperty(element, "font", value);
						break;
					case "rolloverCursor":
						element.css('cursor',value == 12?'pointer':'default');
						break;
					case "mnemonic":
						if (value) element.attr('accesskey',value);
						else element.removeAttr('accesskey');
						break;
					case "horizontalAlignment":
						$svyProperties.setHorizontalAlignment(element.children().first(),value);
						break;
					case "enabled":
						if (value)
							element.removeAttr("disabled");
						else
							element.attr("disabled", "disabled");
						break;
					case "margin":
						if (value)
							element.css(value);
						break;
					case "styleClass":
						if (className)
							element.removeClass(className);
						className = value;
						if (className)
							element.addClass(className);
						break;
					case "toolTipText":
						if (tooltipState)
							tooltipState(value);
						else
							tooltipState = $svyProperties.createTooltipState(element, value);
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
		templateUrl : 'servoydefault/button/button.html'
	};
})
