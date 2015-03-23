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
		        
				$scope.style = {width:'100%',height:'100%',overflow:'hidden'}
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
		         * Sets the display/real values to the custom valuelist of the element (if element has custom valuelist).
		         * This does not affect the value list with same name list on other elements or value lists at application level.
		         * Should receive a dataset parameter, first column is for display values, second column (optional) is for real values.
		         * @example
		         * var dataset = databaseManager.createEmptyDataSet(0,new Array('display_values','optional_real_values'));
		         * dataset.addRow(['aa',1]);
		         * dataset.addRow(['bb',2]);
		         * dataset.addRow(['cc',3]);
		         * // %%prefix%%%%elementName%% should have a valuelist attached
		         * %%prefix%%%%elementName%%.setValueListItems(dataset);
		         *
		         * @param value first column is display value, second column is real value
		         */
				$scope.api.setValueListItems = function(values) 
				{
					var valuelistItems = [];
					for (var i = 0; i < values.length; i++)
					{
						var item = {};
						item['displayValue'] = values[i][0];
						if (values[i][1] !== undefined)
						{
							item['realValue'] = values[i][1];
						}
						valuelistItems.push(item); 
					}
					$scope.model.valuelistID = valuelistItems;
				}
		},
		replace: true
	};
}])





