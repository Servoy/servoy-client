angular.module('servoydefaultSpinner', [ 'servoy' ]).directive('servoydefaultSpinner',
		[ 'formatFilterFilter', '$apifunctions', '$svyProperties', '$sabloConstants', function(formatFilter, $apifunctions, $svyProperties, $sabloConstants) {
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
						height : '100%',
						overflow : 'hidden'
					}
					$scope.findMode = false;
					$scope.selection = getSelectionFromDataprovider();
					$scope.$watch('model.dataProviderID', function() {
						$scope.selection = getSelectionFromDataprovider();
					})

					var input = $element.find('input').eq(0);
					//copied from angularui timepicker
					var isScrollingUp = function(e) {
						if (e.originalEvent) {
							e = e.originalEvent;
						}
						//pick correct delta variable depending on event
						var delta = (e.wheelDelta) ? e.wheelDelta : -e.deltaY;
						return (e.detail || delta > 0);
					};
					input.bind('mousewheel wheel', function(e) {
						if (!$scope.isDisabled()) {
							$scope.$apply((isScrollingUp(e)) ? $scope.increment() : $scope.decrement());
						}
						e.preventDefault();
					});
					input.bind('keydown keypress', function(e) {
						if (!$scope.isDisabled()) {
							if (e.which == 40)
								$scope.decrement();
							if (e.which == 38)
								$scope.increment();
						}
					});

					$scope.isDisabled = function() {
						return $scope.model.enabled == false || $scope.model.editable == false;
					}

					$scope.increment = function() {
						if ($scope.model.valuelistID) {
							$scope.counter = $scope.counter < $scope.model.valuelistID.length - 1 ? $scope.counter + 1 : 0;
							$scope.model.dataProviderID = $scope.model.valuelistID[$scope.counter].realValue
						}
						$scope.svyServoyapi.apply('dataProviderID')
					}

					$scope.decrement = function() {
						if ($scope.model.valuelistID) {
							$scope.counter = $scope.counter > 0 ? $scope.counter - 1 : $scope.model.valuelistID.length - 1;
							$scope.model.dataProviderID = $scope.model.valuelistID[$scope.counter].realValue
						}
						$scope.svyServoyapi.apply('dataProviderID')
					}

					/**
					 * Request the focus to this spinner.
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

					function getSelectionFromDataprovider() {
						if (!$scope.model.dataProviderID) {
							$scope.counter = 0;
							return undefined
						}

						for (var i = 0; i < $scope.model.valuelistID.length; i++) {
							var item = $scope.model.valuelistID[i];
							if (item && item.realValue && $scope.model.dataProviderID == item.realValue) {
								var displayFormat = undefined;
								var type = undefined;
								if ($scope.model.format && $scope.model.format.display)
									displayFormat = $scope.model.format.display;
								if ($scope.model.format && $scope.model.format.type)
									type = $scope.model.format.type;
								$scope.counter = i;
								return formatFilter(item.displayValue, displayFormat, type);
							}
						}
					}

					$scope.api.getWidth = $apifunctions.getWidth($element[0]);
					$scope.api.getHeight = $apifunctions.getHeight($element[0]);
					$scope.api.getLocationX = $apifunctions.getX($element[0]);
					$scope.api.getLocationY = $apifunctions.getY($element[0]);

					var tooltipState = null;
					var className = null;
					var element = $element.children().first();
					
					Object.defineProperty($scope.model, $sabloConstants.modelChangeNotifier, {
						configurable : true,
						value : function(property, value) {
							switch (property) {
							case "toolTipText":
								if (tooltipState)
									tooltipState(value);
								else
									tooltipState = $svyProperties.createTooltipState(element, value);
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
									input.removeAttr("disabled");
								else
									input.attr("disabled", "disabled");
								break;
							case "borderType":
								$svyProperties.setBorder(input, value);
								break;
							case "background":
							case "transparent":
								$svyProperties.setCssProperty(input, "backgroundColor", $scope.model.transparent ? "transparent" : $scope.model.background);
								break;
							case "foreground":
								$svyProperties.setCssProperty(input, "color", value);
								break;
							case "margin":
								if (value)
									input.css(value);
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
				templateUrl : 'servoydefault/spinner/spinner.html'
			};
		} ])
