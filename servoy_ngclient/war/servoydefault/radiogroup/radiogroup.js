angular.module('servoydefaultRadiogroup',['servoy']).directive('servoydefaultRadiogroup', function($utils, $apifunctions) {  
    return {
      restrict: 'E',
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi",
        svyServoyapi: "="
      },
      controller: function($scope, $element, $attrs) {
          $scope.notNullOrEmpty = $utils.notNullOrEmpty // TODO remove the need for this
          $scope.style = {width:'100%',height:'100%'}
          angular.extend($scope.style ,$utils.getScrollbarsStyleObj($scope.model.scrollbars));
          
          /**
        	 * Sets the scroll location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen - only for an element where the height of the element is greater than the height of element content
        	 * NOTE: getScrollX() can be used with getScrollY() to return the current scroll location of an element; then use the X and Y coordinates with the setScroll function to set a new scroll location. For Example:
        	 * //returns the X and Y coordinates
        	 * var x = forms.company.elements.radio50.getScrollX();
        	 * var y = forms.company.elements.radio50.getScrollY();
       	 * //sets the new location
        	 * forms.company.elements.radio50.setScroll(x+10,y+10);
        	 * @example
        	 * %%prefix%%%%elementName%%.setScroll(200,200);
        	 *
        	 * @param x the X coordinate of the radiogroup scroll location in pixels
        	 * @param y the Y coordinate of the radiogroup scroll location in pixels
        	 */
          $scope.api.setScroll = function(x, y) {
         	 $element.scrollLeft(x);
         	 $element.scrollTop(y);
          }
          
          /**
       	  * Returns the x scroll location of specified element - only for an element where height of element is less than the height of element content. 
       	  * NOTE: getScrollX() can be used with getScrollY() to set the scroll location of an element using the setScroll function. For Example:
       	  * //returns the X and Y scroll coordinates
       	  * var x = forms.company.elements.radio50.getScrollX();
       	  * var y = forms.company.elements.radio50.getScrollY(); 
       	  * //sets the new scroll location
       	  * forms.company.elements.radio50.setScroll(x+10,y+10);
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
           * var x = forms.company.elements.radio50.getScrollX();
           * var y = forms.company.elements.radio50.getScrollY();
           * //sets the new scroll location
           * forms.company.elements.radio50.setScroll(x+10,y+10);
           * @example
           * var y = %%prefix%%%%elementName%%.getScrollY(); 
           * @return The y scroll location in pixels.
           */
          $scope.api.getScrollY = function() {
         	 return $element.scrollTop();
          }
          
          
          /**
           * Set the focus to the first radio button.
           * @example %%prefix%%%%elementName%%.requestFocus();
           * @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
           */
          $scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) { 
        	  var input = $element.find('input');
        	  if (input[0])
        	  {
        		  if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID)
        		  {
        			  $(input[0]).unbind('focus');
        			  input[0].focus();
        			  $(input[0]).bind('focus', $scope.handlers.onFocusGainedMethodID)
        		  }
        		  else
        		  {
        			  input[0].focus();
        		  }
        	  }
          }

          /**
      	 * Gets the selected values (real values from valuelist) as array. The form element should have a dataProviderID assigned in order for this to work.
      	 * @example var values = %%prefix%%%%elementName%%.getSelectedElements();
      	 * @return array with selected values
      	 */
          $scope.api.getSelectedElements = function()
          {
        	  var value  = $scope.model.dataProviderID;
        	  if ($scope.model.valuelistID)
        	  {
        		  	for (var i =0;i<$scope.model.valuelistID.length;i++)
        		  	{
        		  		if ($scope.model.valuelistID[i].realValue == value)
        		  		{
        		  			return [value];
        		  		}
        		  	}
        	  }  
        	  return [];
          }
          
       	 $scope.radioClicked = function($event)
       	 {
       		$scope.svyServoyapi.apply('dataProviderID');
       		if($scope.handlers.onFocusLostMethodID) $scope.handlers.onFocusLostMethodID($event);
       	 }
       	 
       	 $scope.api.getWidth = $apifunctions.getWidth($element[0]);
       	 $scope.api.getHeight = $apifunctions.getHeight($element[0]);
       	 $scope.api.getLocationX = $apifunctions.getX($element[0]);
       	 $scope.api.getLocationY = $apifunctions.getY($element[0]);
      },
      templateUrl: 'servoydefault/radiogroup/radiogroup.html'
    };
  })

  
  
  
  
