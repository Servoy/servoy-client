angular.module('servoydefaultListbox', [ 'servoy' ]).
directive('servoydefaultListbox', [ '$parse', '$templateCache', '$http', '$compile', '$apifunctions', '$sabloConstants', '$svyProperties', '$scrollbarConstants', 
                            function($parse, $templateCache, $http, $compile, $apifunctions, $sabloConstants, $svyProperties, $scrollbarConstants) {
	return {
		restrict : 'E',
		scope : {
			model : "=svyModel",
			handlers : "=svyHandlers",
			api : "=svyApi",
			svyServoyapi : "="
		},
		link : function($scope, $element, $attrs) {
			var isMultiSelect = $scope.model.multiselectListbox;
			var templateUrl = isMultiSelect ? "servoydefault/listbox/listbox_multiple.html" : "servoydefault/listbox/listbox.html";
			$http.get(templateUrl, {cache: $templateCache}).then(function(result) {
				$element.html(result.data);
				$compile($element.contents())($scope);
	
				$scope.findMode = false;
	
				$scope.onClick = function(event) {
					var select = $element.find('select');
					var newValue= select.val();
					if (isMultiSelect && newValue)
					{
						newValue = newValue.join('\n');
					}	
					$scope.model.dataProviderID = newValue;
					$scope.svyServoyapi.apply('dataProviderID');
					if ($scope.handlers.onActionMethodID)
					{
						$scope.handlers.onActionMethodID(event)
					}
				}
				/**
				 * Sets the scroll location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen - only for an element where the height of the element is greater than the height of element content
				 * NOTE: getScrollX() can be used with getScrollY() to return the current scroll location of an element; then use the X and Y coordinates with the setScroll function to set a new scroll location. 
				 * For Example:
				 * //returns the X and Y coordinates
				 * var x = forms.company.elements.mylist.getScrollX();
				 * var y = forms.company.elements.mylist.getScrollY();
				 * //sets the new location
				 * forms.company.elements.mylist.setScroll(x+10,y+10);
				 * @example
				 * %%prefix%%%%elementName%%.setScroll(200,200);
				 *
				 * @param x the X coordinate of the listbox scroll location in pixels
				 * @param y the Y coordinate of the listbox scroll location in pixels
				 */
				$scope.api.setScroll = function(x, y) {
					$element.scrollLeft(x);
					$element.scrollTop(y);
				}
	
				/**
				  * Returns the x scroll location of specified element - only for an element where height of element is less than the height of element content. 
				  * NOTE: getScrollX() can be used with getScrollY() to set the scroll location of an element using the setScroll function. 
				  * For Example:
				  * //returns the X and Y scroll coordinates
				  * var x = forms.company.elements.mylist.getScrollX();
				  * var y = forms.company.elements.mylist.getScrollY(); 
				  * //sets the new scroll location
				  * forms.company.elements.mylist.setScroll(x+10,y+10);
				  * @example
				  * var x = %%prefix%%%%elementName%%.getScrollX();
				  * 
				  * @return The x scroll location in pixels.
				  */
				$scope.api.getScrollX = function() {
					return $element.scrollLeft();
				}
	
				/**
				    * Returns the y scroll location of specified element - only for an element where height of element is less than the height of element content.
				    * NOTE: getScrollY() can be used with getScrollX() to set the scroll location of an element using the setScroll function. For Example:
				    * //returns the X and Y scroll coordinates
				    * var x = forms.company.elements.mylist.getScrollX();
				    * var y = forms.company.elements.mylist.getScrollY();
				    * //sets the new scroll location
				    * forms.company.elements.mylist.setScroll(x+10,y+10);
				    * @example
				    * var y = %%prefix%%%%elementName%%.getScrollY(); 
				    * @return The y scroll location in pixels.
				    */
				$scope.api.getScrollY = function() {
					return $element.scrollTop();
				}
	
				/**
				   * Set the focus to the listbox.
				   * @example %%prefix%%%%elementName%%.requestFocus();
				   * @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
				   */
				$scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) {
					var select = $element.find('select');
					if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID) {
						select.unbind('focus');
						select[0].focus();
						select.bind('focus', $scope.handlers.onFocusGainedMethodID)
					} else {
						select[0].focus();
					}
				}
	
				/**
				 * Gets the selected values (real values from valuelist) as array. The form element should have a dataProviderID assigned in order for this to work.
				 * @example var values = %%prefix%%%%elementName%%.getSelectedElements();
				 * @return array with selected values
				 */
				$scope.api.getSelectedElements = function() {
					var value = [];
					if ($scope.model.valuelistID) {
						for (var i = 0; i < $scope.model.valuelistID.length; i++) {
							if ($scope.convertModel.indexOf($scope.model.valuelistID[i].realValue) >= 0) {
								value.push($scope.model.valuelistID[i].realValue);
							}
						}
					}
					return value;
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
						case "scrollbars":
							$svyProperties.setScrollbars(element, value);
							break;
						case "fontType":
							$svyProperties.setCssProperty(element, "font", value);
							break;
						case "enabled":
							if (value)
								element.removeAttr("disabled");
							else
								element.attr("disabled", "disabled");
							break;
						case "horizontalAlignment":
							$svyProperties.setHorizontalAlignment(element.children().first(), value);
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
				// data can already be here, if so call the modelChange function so that it is initialized correctly.
				var modelChangFunction = $scope.model[$sabloConstants.modelChangeNotifier];
				for (key in $scope.model) {
					modelChangFunction(key, $scope.model[key]);
				}
			});
		},
		replace : true
	};
} ])
