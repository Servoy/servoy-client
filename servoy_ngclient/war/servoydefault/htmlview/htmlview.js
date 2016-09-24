angular.module('servoydefaultHtmlview',['servoy']).directive('servoydefaultHtmlview', function($apifunctions, $sabloConstants, $svyProperties) {  
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      	api: "=svyApi",
      	handlers: "=svyHandlers",
		servoyApi: "=svyServoyapi"
      },
      link: function($scope, $element, $attrs,ngModelController) {
//       $scope.style = {width:'100%',height:'100%',overflow:'auto'}
//       $scope.bgstyle = {left:'0',right:'0',top:'0',height:'100%',position:'relative',display:'block',};
       
       /**
     	 * Sets the scroll location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen - only for an element where the height of the element is greater than the height of element content
     	 * NOTE: getScrollX() can be used with getScrollY() to return the current scroll location of an element; then use the X and Y coordinates with the setScroll function to set a new scroll location. 
     	 * For Example:
     	 * //returns the X and Y coordinates
     	 * var x = forms.company.elements.myview.getScrollX();
     	 * var y = forms.company.elements.myview.getScrollY();
    	 * //sets the new location
     	 * forms.company.elements.myview.setScroll(x+10,y+10);
     	 * @example
     	 * %%prefix%%%%elementName%%.setScroll(200,200);
     	 *
     	 * @param x the X coordinate of the htmlview scroll location in pixels
     	 * @param y the Y coordinate of the htmlview scroll location in pixels
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
    	  * var x = forms.company.elements.myview.getScrollX();
    	  * var y = forms.company.elements.myview.getScrollY(); 
    	  * //sets the new scroll location
    	  * forms.company.elements.myview.setScroll(x+10,y+10);
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
        * var x = forms.company.elements.myview.getScrollX();
        * var y = forms.company.elements.myview.getScrollY();
        * //sets the new scroll location
        * forms.company.elements.myview.setScroll(x+10,y+10);
        * @example
        * var y = %%prefix%%%%elementName%%.getScrollY(); 
        * @return The y scroll location in pixels.
        */
       $scope.api.getScrollY = function() {
      	 return $element.scrollTop();
       }

       /**
        * Gets the plain text for the formatted Html view.
        * @example var my_text = %%prefix%%%%elementName%%.getAsPlainText();
        * @return the plain text
        */
       $scope.api.getAsPlainText = function() {
    	   if ($scope.model.dataProviderID)
    	   {
    		   return $element.text().trim().replace(/^ +/gm, '');
    	   }
    	   return null;
       }
       
       $scope.api.getWidth = $apifunctions.getWidth($element[0]);
       $scope.api.getHeight = $apifunctions.getHeight($element[0]);
       $scope.api.getLocationX = $apifunctions.getX($element[0]);
       $scope.api.getLocationY = $apifunctions.getY($element[0]);
       
		var element = $element.children().first();
		var tooltipState = null;
		var className = null;
		Object.defineProperty($scope.model, 	$sabloConstants.modelChangeNotifier, {
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
				case "toolTipText":
					if (tooltipState)
						tooltipState(value);
					else
						tooltipState = $svyProperties.createTooltipState(element, value);
					break;
				case "fontType":
					$svyProperties.setCssProperty(element, "font", value);
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
      templateUrl: 'servoydefault/htmlview/htmlview.html'
 };
})