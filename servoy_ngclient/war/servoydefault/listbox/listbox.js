angular.module('servoydefaultListbox',['servoy'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("servoydefault/listbox/listbox.html").then(function(result){
		$templateCache.put("servoydefault/listbox/listbox.html", result.data);
    });
	$http.get("servoydefault/listbox/listbox_multiple.html").then(function(result){
		$templateCache.put("servoydefault/listbox/listbox_multiple.html", result.data);
    });	
}]).directive('servoydefaultListbox', ['$parse','$templateCache','$compile',function($parse,$templateCache,$compile) {  
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			handlers: "=svyHandlers",
			api: "=svyApi",
			svyServoyapi: "="
		},
		link: function($scope, $element, $attrs) {
				var isMultiSelect = $scope.model.multiselectListbox;
				$element.html($templateCache.get(isMultiSelect ? "servoydefault/listbox/listbox_multiple.html" : "servoydefault/listbox/listbox.html"));
		        $compile($element.contents())($scope);
		        
				$scope.style = {width:'100%',height:'100%'}
				$scope.findMode = false;
				$scope.$watch('model.dataProviderID', function() {
					if(!$scope.model.dataProviderID)
					{
						$scope.convertModel = null;
					}
					else
					{	
						// TODO needs to be automatic
						if(isMultiSelect){
							$scope.convertModel = ($scope.model.dataProviderID+'').split('\n');	
						}else{
							$scope.convertModel = $scope.model.dataProviderID
						}
					}
				})
				$scope.$watch('convertModel', function() {
					var oldValue = $scope.model.dataProviderID;
					var newValue = null;
					if(!$scope.convertModel)
					{
						newValue = null;
					}
					else
					{
						if(isMultiSelect){
							newValue = $scope.convertModel.join('\n');	
						}else{
							newValue = $scope.convertModel;	
						}						 
					}
					if (oldValue != newValue)
					{
						$scope.model.dataProviderID = newValue;
						$scope.svyServoyapi.apply('dataProviderID');
					}	  
				})

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
					if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID)
					{
						select.unbind('focus');
						select[0].focus();
						select.bind('focus', $scope.handlers.onFocusGainedMethodID)
					}
					else
					{
						select[0].focus();
					}
				}
				
				 /**
		      	 * Gets the selected values (real values from valuelist) as array. The form element should have a dataProviderID assigned in order for this to work.
		      	 * @example var values = %%prefix%%%%elementName%%.getSelectedElements();
		      	 * @return array with selected values
		      	 */
		          $scope.api.getSelectedElements = function()
		          {
		        	  var value  = [];
		        	  if ($scope.model.valuelistID)
		        	  {
		        		  	for (var i =0;i<$scope.model.valuelistID.length;i++)
		        		  	{
		        		  		if ($scope.convertModel.indexOf($scope.model.valuelistID[i].realValue) >= 0)
		        		  		{
		        		  			value.push($scope.model.valuelistID[i].realValue);
		        		  		}
		        		  	}
		        	  }  
		        	  return value;
		          }
		          
		},
		replace: true
	};
}])





