angular.module('servoydefaultCheckgroup',['servoy']).directive('servoydefaultCheckgroup', function($utils, $apifunctions, $svyProperties, $sabloConstants, $scrollbarConstants) {  
    return {
      restrict: 'E',
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi",
        svyServoyapi: "="
      },
      link: function($scope, $element, $attrs) {
         $scope.notNullOrEmpty = $utils.notNullOrEmpty  // adding it to the root scope doesn't fix the resolution of the comparator in the filter (in this directive). it has to be in local scope. TODO remove the need for this
         $element.children().first().css($svyProperties.getScrollbarsStyleObj($scope.model.scrollbars));
//         angular.extend($scope.style,);

          var allowNullinc=0;
          
          $scope.selection= []
          
          $scope.$watch('model.dataProviderID', function() { 
             setSelectionFromDataprovider();
          })
          $scope.$watch('model.valuelistID',function() {
            if ($scope.svyServoyapi.isInDesigner() && !$scope.model.valuelistID) {
        	   $scope.model.valuelistID = [{realValue:1,displayValue:"Item1"},{realValue:2,displayValue:"Item2"},{realValue:3,displayValue:"Item3"}];
            }
            if(!$scope.model.valuelistID) return; // not loaded yet
            if(isValueListNull($scope.model.valuelistID[0])) allowNullinc=1;
            setSelectionFromDataprovider();
          })
          
          
          $scope.checkBoxClicked = function($event,$index){
             var checkedTotal = 0;
             for(var i=0;i< $scope.selection.length ;i++){
            	 if($scope.selection[i]==true) checkedTotal++;            	 
             }
             var allowMultiselect = !$scope.model.format || $scope.model.format.type == "TEXT";
             if (!allowMultiselect && checkedTotal > 1)
             {
            	 for(var i=0;i< $scope.selection.length ;i++){
                	 if($scope.selection[i]==true) $scope.selection[i] = false;            	 
                 }
            	 $scope.selection[$index] = true; 
             }
            // prevent unselection of the last element if 'allow null' is not set                                          
            if(checkedTotal==0 && allowNullinc ==0){
               $scope.selection[$index] = true;
            }
            $scope.model.dataProviderID = getDataproviderFromSelection()
            
            if(checkedTotal==0 && allowNullinc ==0) return;// only push if it was actualy changed
            $scope.svyServoyapi.apply('dataProviderID')        
            if($scope.handlers.onFocusLostMethodID) $scope.handlers.onFocusLostMethodID($event)
          }
          
         /**
      	 * Sets the scroll location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen - only for an element where the height of the element is greater than the height of element content
      	 * NOTE: getScrollX() can be used with getScrollY() to return the current scroll location of an element; then use the X and Y coordinates with the setScroll function to set a new scroll location. For Example:
      	 * //returns the X and Y coordinates
      	 * var x = forms.company.elements.check50.getScrollX();
      	 * var y = forms.company.elements.check50.getScrollY();
     	 * //sets the new location
      	 * forms.company.elements.check50.setScroll(x+10,y+10);
      	 * @example
      	 * %%prefix%%%%elementName%%.setScroll(200,200);
      	 *
      	 * @param x the X coordinate of the checkgroup scroll location in pixels
      	 * @param y the Y coordinate of the checkgroup scroll location in pixels
      	 */
         $scope.api.setScroll = function(x, y) {
         	 $element.scrollLeft(x);
         	 $element.scrollTop(y);
         }
          
          /**
      	  * Returns the x scroll location of specified element - only for an element where height of element is less than the height of element content. 
      	  * NOTE: getScrollX() can be used with getScrollY() to set the scroll location of an element using the setScroll function. For Example:
      	  * //returns the X and Y scroll coordinates
      	  * var x = forms.company.elements.check50.getScrollX();
      	  * var y = forms.company.elements.check50.getScrollY(); 
      	  * //sets the new scroll location
      	  * forms.company.elements.check50.setScroll(x+10,y+10);
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
           * var x = forms.company.elements.check50.getScrollX();
           * var y = forms.company.elements.check50.getScrollY();
           * //sets the new scroll location
           * forms.company.elements.check50.setScroll(x+10,y+10);
           * @example
           * var y = %%prefix%%%%elementName%%.getScrollY(); 
           * @return The y scroll location in pixels.
           */
          $scope.api.getScrollY = function() {
        	  return $element.scrollTop();
          }
          
          /**
           * Set the focus to the first checkbox.
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
            	var ret = [];
            	$scope.selection.forEach(function(element, index, array){
            		if(element == true) ret.push($scope.model.valuelistID[index+allowNullinc].realValue);
            	});
            	return ret;
            }
            
    /* helper functions*/
          function setSelectionFromDataprovider(){
        	$scope.selection =[]  
            if($scope.model.dataProviderID === null || $scope.model.dataProviderID === undefined) return;
            var arr = (typeof $scope.model.dataProviderID ==="string") ? $scope.model.dataProviderID.split('\n') : [$scope.model.dataProviderID];
            arr.forEach(function(element, index, array){
                for(var i=0;i<$scope.model.valuelistID.length;i++){
                  var item= $scope.model.valuelistID[i];
                    if(item.realValue==element && !isValueListNull(item)) $scope.selection[i-allowNullinc] = true;
                }
            });
          }
          
          function getDataproviderFromSelection(){
        	var allowMultiselect = !$scope.model.format || $scope.model.format.type == "TEXT";
            var ret = allowMultiselect ? "" : null;
            $scope.selection.forEach(function(element, index, array){
               // if(index == array.length-allowNullinc) return;
                if(element == true)
                	if (allowMultiselect)
                	{
                		ret+= $scope.model.valuelistID[index+allowNullinc].realValue+'\n';
                	}
                	else
                	{
                		ret = $scope.model.valuelistID[index+allowNullinc].realValue
                	}
            });
              if (allowMultiselect) ret = ret.replace(/\n$/, "");//remove the last \n
              if(ret === "") ret = null
              return ret;
          }
          
          function isValueListNull(item)
          {
              return (item.realValue == null || item.realValue =='') && item.displayValue=='';
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
					case "horizontalAlignment":
						$svyProperties.setHorizontalAlignment(element, value);
						break;
					case "scrollbars":
						element.removeClass('horizontaldirection');
						if ((value & $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) == $scrollbarConstants.VERTICAL_SCROLLBAR_NEVER) {// vertical scrollbar never
							element.addClass('horizontaldirection');
						}
						$svyProperties.setScrollbars(element, value);
						break;
					case "toolTipText":
						if (tooltipState)
							tooltipState(value);
						else
							tooltipState = $svyProperties.createTooltipState(element, value);
						break;
//					case "enabled":
//						if (value)
//							element.removeAttr("disabled");
//						else
//							element.attr("disabled", "disabled");
//						break;
//					case "editable":
//						if (value)
//							element.removeAttr("disabled");
//						else
//							element.attr("disabled", "disabled");
//						break;
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
      templateUrl: 'servoydefault/checkgroup/checkgroup.html'
    };
  })

  
  
  
  
