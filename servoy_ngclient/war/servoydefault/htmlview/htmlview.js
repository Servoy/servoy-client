angular.module('servoydefaultHtmlview',['servoy']).directive('servoydefaultHtmlview', function() {  
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      	api: "=svyApi",
      	handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs,ngModelController) {
       $scope.style = {width:'100%',height:'100%',overflow:'auto'}
       $scope.bgstyle = {left:'0',right:'0',top:'0',height:'100%',position:'relative',display:'block',};
       
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
      },
      templateUrl: 'servoydefault/htmlview/htmlview.html'
 };
})