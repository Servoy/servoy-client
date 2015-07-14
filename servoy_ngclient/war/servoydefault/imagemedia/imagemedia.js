angular.module('servoydefaultImagemedia', [ 'servoy' ]).directive(
		'servoydefaultImagemedia',
		function($window, $document, $apifunctions, $sabloConstants, $svyProperties) {
			return {
				restrict : 'E',
				scope : {
					model : "=svyModel",
					api : "=svyApi",
					svyServoyapi : "="
				},
				controller : function($scope, $element, $attrs) {

					$scope.imageURL = '';

					$scope.$watch('model.dataProviderID', function() {
						if ($scope.model.dataProviderID && !$scope.model.dataProviderID.url) {
							// plain media url
							$scope.imageURL = $scope.model.dataProviderID;
						} else {
							$scope.imageURL = ($scope.model.dataProviderID && $scope.model.dataProviderID.url) ? ($scope.model.dataProviderID.contentType
									&& ($scope.model.dataProviderID.contentType.indexOf("image") == 0) ? $scope.model.dataProviderID.url : "servoydefault/imagemedia/res/images/notemptymedia.gif")
									: "servoydefault/imagemedia/res/images/empty.gif";
						}

					})

					$scope.download = function() {
						if ($scope.model.dataProviderID) {
							var x = 0, y = 0;
							if ($document.all) {
								x = $window.screenTop + 100;
								y = $window.screenLeft + 100;
							} else if ($document.layers) {
								x = $window.screenX + 100;
								y = $window.screenY + 100;
							} else { // firefox, need to switch the x and y?
								y = $window.screenX + 100;
								x = $window.screenY + 100;
							}
							$window.open($scope.model.dataProviderID.url ? $scope.model.dataProviderID.url : $scope.model.dataProviderID, 'download', 'top=' + x + ',left=' + y + ',screenX=' + x
									+ ',screenY=' + y + ',location=no,toolbar=no,menubar=no,width=310,height=140,resizable=yes');
						}
					}

					$scope.clear = function() {
						$scope.model.dataProviderID = null;
						$scope.svyServoyapi.apply('dataProviderID');
					}

					/**
					 * Sets the scroll location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen - only for an element where the height of the element is greater than the height of element content
					 * NOTE: getScrollX() can be used with getScrollY() to return the current scroll location of an element; then use the X and Y coordinates with the setScroll function to set a new scroll location. 
					 * For Example:
					 * //returns the X and Y coordinates
					 * var x = forms.company.elements.myimage.getScrollX();
					 * var y = forms.company.elements.myimage.getScrollY();
					 * //sets the new location
					 * forms.company.elements.myimage.setScroll(x+10,y+10);
					 * @example
					 * %%prefix%%%%elementName%%.setScroll(200,200);
					 *
					 * @param x the X coordinate of the imagemedia scroll location in pixels
					 * @param y the Y coordinate of the imagemedia scroll location in pixels
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
					 * var x = forms.company.elements.myimage.getScrollX();
					 * var y = forms.company.elements.myimage.getScrollY(); 
					 * //sets the new scroll location
					 * forms.company.elements.myimage.setScroll(x+10,y+10);
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
					 * var x = forms.company.elements.myimage.getScrollX();
					 * var y = forms.company.elements.myimage.getScrollY();
					 * //sets the new scroll location
					 * forms.company.elements.myimage.setScroll(x+10,y+10);
					 * @example
					 * var y = %%prefix%%%%elementName%%.getScrollY(); 
					 * @return The y scroll location in pixels.
					 */
					$scope.api.getScrollY = function() {
						return $element.scrollTop();
					}

					$scope.api.getWidth = $apifunctions.getWidth($element[0]);
					$scope.api.getHeight = $apifunctions.getHeight($element[0]);
					$scope.api.getLocationX = $apifunctions.getX($element[0]);
					$scope.api.getLocationY = $apifunctions.getY($element[0]);

					var element = $element.children().first();
					var tooltipState = null;
					var className = null;
					Object.defineProperty($scope.model, $sabloConstants.modelChangeNotifier, {
						configurable : true,
						value : function(property, value) {
							switch (property) {
							case "borderType":
								$svyProperties.setBorder(element, value);
								break;
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
							}
						}
					});
					var destroyListenerUnreg = $scope.$on("$destroy", function() {
						destroyListenerUnreg();
						delete scope.model[$sabloConstants.modelChangeNotifier];
					});
					// data can already be here, if so call the modelChange function so
					// that it is initialized correctly.
					var modelChangFunction = $scope.model[$sabloConstants.modelChangeNotifier];
					for (key in $scope.model) {
						modelChangFunction(key, $scope.model[key]);
					}
				},
				templateUrl : 'servoydefault/imagemedia/imagemedia.html'
			};
		})