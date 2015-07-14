angular.module('servoydefaultRectangle', [ 'servoy' ]).directive('servoydefaultRectangle', function($apifunctions, $svyProperties, $sabloConstants) {
	return {
		restrict : 'E',
		scope : {
			model : "=svyModel",
			handlers : "=svyHandlers",
			api : "=svyApi"
		},
		link : function($scope, $element, $attrs) {

			function setupRectangle() {
				if (!$scope.model.borderType) {
					$scope.model.borderType = new Object();
					$scope.model.borderType.borderStyle = {
						borderStyle : "solid"
					};
				}
				if ($scope.model.borderType.borderStyle) {
					$scope.model.borderType.borderStyle.borderWidth = $scope.model.lineSize + "px";
					$scope.model.borderType.borderStyle.borderColor = $scope.model.foreground ? $scope.model.foreground : "#000000";
				}

				if ($scope.model.roundedRadius) {
					$scope.model.borderType.borderStyle.borderRadius = $scope.model.roundedRadius / 2 + "px";
				} else if ($scope.model.shapeType == 3) {
					$scope.model.borderType.borderStyle.borderRadius = $scope.model.size.width / 2 + "px";
				}
			}

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
						setupRectangle();
						$svyProperties.setBorder(element, $scope.model.borderType);
						break;
					case "background":
					case "transparent":
						$svyProperties.setCssProperty(element, "backgroundColor", $scope.model.transparent ? "transparent" : $scope.model.background);
						break;
					case "foreground":
						if ($scope.model.borderType && $scope.model.borderType.borderStyle) {
							$scope.model.borderType.borderStyle.borderColor = $scope.model.foreground ? $scope.model.foreground : "#000000";
						}
						setupRectangle();
						$svyProperties.setBorder(element, $scope.model.borderType);
						break;
					case "styleClass":
						if (className)
							element.removeClass(className);
						className = value;
						if (className)
							element.addClass(className);
						break;
					case "size":
						if ($scope.model.shapeType == 3 && $scope.model.borderType && $scope.model.borderType.borderStyle) {
							$scope.model.borderType.borderStyle.borderRadius = $scope.model.size.width / 2 + "px";
						}
						setupRectangle();
						$svyProperties.setBorder(element, $scope.model.borderType);
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
		templateUrl : 'servoydefault/rectangle/rectangle.html'
	};
})