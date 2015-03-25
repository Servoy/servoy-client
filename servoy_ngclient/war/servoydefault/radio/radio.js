angular.module('servoydefaultRadio',['servoy']).directive('servoydefaultRadio', function() {  
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
          $scope.style = {width:'100%',height:'100%'}  
          
          $scope.radioClicked = function()
          {
        	  $scope.model.dataProviderID = $scope.model.valuelistID[0].realValue;
        	  $scope.svyServoyapi.apply('dataProviderID')
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
          
          /**
           * Request the focus to this radio button.
           * @example %%prefix%%%%elementName%%.requestFocus();
           * @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
           */
          $scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) { 
        	  var input = $element.find('input');
        	  if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID)
        	  {
        		  input.unbind('focus');
        		  input[0].focus();
        		  input.bind('focus', $scope.handlers.onFocusGainedMethodID)
        	  }
        	  else
        	  {
        		  input[0].focus();
        	  }
          }
      },
      templateUrl: 'servoydefault/radio/radio.html'
    };
})