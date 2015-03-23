angular.module('servoydefaultCheckgroup',['servoy']).directive('servoydefaultCheckgroup', function($utils) {  
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
         $scope.style = {width:'100%',height:'100%'}
         angular.extend($scope.style ,$utils.getScrollbarsStyleObj($scope.model.scrollbars));

          var allowNullinc=0;
          
          $scope.selection= []
          
          $scope.$watch('model.dataProviderID', function() { 
             setSelectionFromDataprovider();
          })
          $scope.$watch('model.valuelistID',function() {
            if(!$scope.model.valuelistID) return; // not loaded yet
            if(isValueListNull($scope.model.valuelistID[0])) allowNullinc=1;
            setSelectionFromDataprovider();
          })
          
          $scope.checkBoxClicked = function($event,$index){
             var checkedTotal = 0;
             for(var i=0;i< $scope.selection.length ;i++){
            	 if($scope.selection[i]==true) checkedTotal++;            	 
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
         * Sets the display/real values to the custom valuelist of the element (if element has custom valuelist).
         * This does not affect the value list with same name list on other elements or value lists at application level.
         * Should receive a dataset parameter, first column is for display values, second column (optional) is for real values.
         * NOTE: if you modify values for checkbox field, note that having one value in valuelist is a special case, so switching between one value and 0/multiple values after form is created may have side effects
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
          
          
    /* helper functions*/
          function setSelectionFromDataprovider(){
            if(!$scope.model.dataProviderID) return
            $scope.selection =[]
            var arr = $scope.model.dataProviderID.split('\n')
            arr.forEach(function(element, index, array){
                for(var i=0;i<$scope.model.valuelistID.length;i++){
                  var item= $scope.model.valuelistID[i];
                    if(item.realValue && item.realValue==element && !isValueListNull(item)) $scope.selection[i-allowNullinc] = true;
                }
            });
          }
          
          function getDataproviderFromSelection(){
            var ret ="";
            $scope.selection.forEach(function(element, index, array){
               // if(index == array.length-allowNullinc) return;
                if(element == true) ret+= $scope.model.valuelistID[index+allowNullinc].realValue+'\n';
            });
              if(ret =="") ret =null
              return ret;
          }
          
          function isValueListNull(item){
              if(item.realValue == null && item.displayValue=='')
              {return true;}
              else return false
          }
      },
      templateUrl: 'servoydefault/checkgroup/checkgroup.html'
    };
  })

  
  
  
  
