angular.module('servoydefaultTextarea', [ 'servoy' ]).directive('servoydefaultTextarea', function($apifunctions, $svyProperties, $formatterUtils, $sabloConstants) {
	return {
		restrict : 'E',
		require : 'ngModel',
		scope : {
			model : "=svyModel",
			api : "=svyApi",
			handlers : "=svyHandlers"
		},
		link : function($scope, $element, $attrs, ngModel) {
//			$scope.style = {
//				width : '100%',
//				height : '100%',
//				resize : 'none'
//			}
			$scope.findMode = false;

			var storedTooltip = false;
			// fill in the api defined in the spec file
			$scope.api.onDataChangeCallback = function(event, returnval) {
				var stringValue = typeof returnval == 'string'
				if (returnval === false || stringValue) {
					$element[0].focus();
					ngModel.$setValidity("", false);
					if (stringValue) {
						if (storedTooltip == false)
							storedTooltip = $scope.model.toolTipText;
						$scope.model.toolTipText = returnval;
					}
				} else {
					ngModel.$setValidity("", true);
					if (storedTooltip !== false)
						$scope.model.toolTipText = storedTooltip;
					storedTooltip = false;
				}
			}
			/**
			 * Request the focus to this textarea.
			 * @example %%prefix%%%%elementName%%.requestFocus();
			 * @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
			 */
			$scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) {
				if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID) {
					$element.unbind('focus');
					$element[0].focus();
					$element.bind('focus', $scope.handlers.onFocusGainedMethodID)
				} else {
					$element[0].focus();
				}
			}

			/**
			 * Sets the scroll location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen - only for an element where the height of the element is greater than the height of element content
			 * NOTE: getScrollX() can be used with getScrollY() to return the current scroll location of an element; then use the X and Y coordinates with the setScroll function to set a new scroll location. 
			 * For Example:
			 * //returns the X and Y coordinates
			 * var x = forms.company.elements.mytextarea.getScrollX();
			 * var y = forms.company.elements.mytextarea.getScrollY();
			 * //sets the new location
			 * forms.company.elements.mytextarea.setScroll(x+10,y+10);
			 * @example %%prefix%%%%elementName%%.setScroll(200,200);
			 * @param x the X coordinate of the textarea scroll location in pixels
			 * @param y the Y coordinate of the textarea scroll location in pixels
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
			 * var x = forms.company.elements.mytextarea.getScrollX();
			 * var y = forms.company.elements.mytextarea.getScrollY(); 
			 * //sets the new scroll location
			 * forms.company.elements.mytextarea.setScroll(x+10,y+10);
			 * @example var x = %%prefix%%%%elementName%%.getScrollX();
			  * @return The x scroll location in pixels.
			  */
			$scope.api.getScrollX = function() {
				return $element.scrollLeft();
			}

			/**
			 * Returns the y scroll location of specified element - only for an element where height of element is less than the height of element content.
			 * NOTE: getScrollY() can be used with getScrollX() to set the scroll location of an element using the setScroll function. For Example:
			 * //returns the X and Y scroll coordinates
			 * var x = forms.company.elements.mytextarea.getScrollX();
			 * var y = forms.company.elements..getScmytextarearollY();
			 * //sets the new scroll location
			 * forms.company.elements.mytextarea.setScroll(x+10,y+10);
			 * @example var y = %%prefix%%%%elementName%%.getScrollY(); 
			 * @return The y scroll location in pixels.
			 */
			$scope.api.getScrollY = function() {
				return $element.scrollTop();
			}

			/**
			 * Returns the currently selected text in the specified text area. 
			 * @example var my_text = %%prefix%%%%elementName%%.getSelectedText();
			 * @return {String} The selected text from the component.
			 */
			$scope.api.getSelectedText = $apifunctions.getSelectedText($element[0]);
			$scope.api.setSelection = $apifunctions.setSelection($element[0]);
			/**
			 * Replaces the selected text; if no text has been selected, the replaced value will be inserted at the last cursor position.
			 * @example %%prefix%%%%elementName%%.replaceSelectedText('John');
			 * @param s The replacement text.
			 */
			$scope.api.replaceSelectedText = $apifunctions.replaceSelectedText($element[0]);
			/**
			 * Selects all the contents of the textarea.
			 * @example %%prefix%%%%elementName%%.selectAll();
			 */
			$scope.api.selectAll = $apifunctions.selectAll($element[0]);

			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);

			var tooltipState = null;
			var formatState = null;
			var className = null;
			Object.defineProperty($scope.model, $sabloConstants.modelChangeNotifier, {
				configurable : true,
				value : function(property, value) {
					switch (property) {
					case "fontType":
						$svyProperties.setCssProperty($element, "font", value);
						break;
					case "background":
					case "transparent":
						$svyProperties.setCssProperty($element, "backgroundColor", $scope.model.transparent ? "transparent" : $scope.model.background);
						break;
					case "foreground":
						$svyProperties.setCssProperty($element, "color", value);
						break;
					case "scrollbars":
						$svyProperties.setScrollbars($element, value);
						break;
					case "margin":
						if (value)
							$element.css(value);
						break;
					case "styleClass":
						if (className)
							$element.removeClass(className);
						className = value;
						if (className)
							$element.addClass(className);
						break;
					case "borderType":
						$svyProperties.setBorder($element, value);
						break;
					case "enabled":
						if (value)
							$element.removeAttr("disabled");
						else
							$element.attr("disabled", "disabled");
						break;
					case "editable":
						if (value)
							$element.removeAttr("readonly");
						else
							$element.attr("readonly", "readonly");
						break;
					case "toolTipText":
						if (tooltipState)
							tooltipState(value);
						else
							tooltipState = $svyProperties.createTooltipState($element, value);
						break;
					case "format":
						if (formatState)
							formatState(value);
						else
							formatState = $formatterUtils.createFormatState($element, $scope, ngModel, true, value);
						break;
					case "horizontalAlignment":
						$svyProperties.setHorizontalAlignment($element, value);
						break;
					case "placeholderText":
						if (value)
							$element.attr("placeholder", value)
						else
							$element.removeAttr("placeholder");
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
		templateUrl : 'servoydefault/textarea/textarea.html',
		replace : true
	};
})
