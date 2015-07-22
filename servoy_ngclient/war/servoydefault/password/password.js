angular.module('servoydefaultPassword', [ 'servoy' ]).directive('servoydefaultPassword', function($apifunctions,$svyProperties,$formatterUtils,$sabloConstants) {
	return {
		restrict : 'E',
		scope : {
			model : "=svyModel",
			api : "=svyApi",
			handlers : "=svyHandlers"
		},
		controller : function($scope, $element, $attrs) {
			$scope.findMode = false;

			$scope.onClick = function(event) {
				if ($scope.model.editable == false && $scope.handlers.onActionMethodID) {
					$scope.handlers.onActionMethodID(event);
				}
			}

			// fill in the api defined in the spec file
			$scope.api.onDataChangeCallback = function(event, returnval) {
				if (returnval === false) {
					$element[0].childNodes[0].focus();
				}
			},
			/**
			* Request the focus in this password field.
			* @example %%prefix%%%%elementName%%.requestFocus();
			*/
			$scope.api.requestFocus = function() {
				$element[0].childNodes[0].focus()
			}

			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);

			var tooltipState = null;
			var formatState = null;
			var className = null;
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
					case "margin":
						if (value)
							element.css(value);
						break;
					case "selectOnEnter":
						if (value)
							$svyProperties.addSelectOnEnter(element);
						break;
					case "styleClass":
						if (className)
							element.removeClass(className);
						className = value;
						if (className)
							element.addClass(className);
						break;
					case "enabled":
						if (value)
							element.removeAttr("disabled");
						else
							element.attr("disabled", "disabled");
						break;
					case "editable":
						if (value)
							element.removeAttr("readonly");
						else
							element.attr("readonly", "readonly");
						break;
					case "toolTipText":
						if (tooltipState)
							tooltipState(value);
						else
							tooltipState = $svyProperties.createTooltipState(element, value);
						break;
					case "horizontalAlignment":
						$svyProperties.setHorizontalAlignment(element, value);
						break;
					case "format":
						if (formatState)
							formatState(value);
						else
							formatState = $formatterUtils.createFormatState(element, $scope, ngModel, true, value);
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
		templateUrl : 'servoydefault/password/password.html'
	};
})
