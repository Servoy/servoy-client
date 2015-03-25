angular.module('servoydefaultSpinner',['servoy']).directive('servoydefaultSpinner',['formatFilterFilter', function(formatFilter) {  
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
    	  $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	  $scope.findMode = false;
          $scope.selection = getSelectionFromDataprovider();
          $scope.$watch('model.dataProviderID', function() 
          { 
        	  $scope.selection = getSelectionFromDataprovider();
          })
          
          var input = $element.find('input').eq(0);
          //copied from angularui timepicker
          var isScrollingUp = function(e) {
	          if (e.originalEvent) {
	            e = e.originalEvent;
	          }
	          //pick correct delta variable depending on event
	          var delta = (e.wheelDelta) ? e.wheelDelta : -e.deltaY;
	          return (e.detail || delta > 0);
          };
          input.bind('mousewheel wheel', function(e) {
        	  if (!$scope.isDisabled())
        	  {
        		  $scope.$apply( (isScrollingUp(e)) ? $scope.increment() : $scope.decrement() );
        	  }
              e.preventDefault();
          });
          input.bind('keydown keypress', function(e){
        	  if (!$scope.isDisabled())
        	  {
	        	  if (e.which == 40) $scope.decrement();
	        	  if (e.which == 38) $scope.increment();
        	  }
        	  if(e.which != 9) e.preventDefault(); // 9 is tab key
          });
          
          $scope.isDisabled = function()
          {
        	  return $scope.model.enabled == false || $scope.model.editable == false;
          }
        	  
          $scope.increment = function()
          {
        	  if ($scope.model.valuelistID)
        	  {
        		  $scope.counter = $scope.counter < $scope.model.valuelistID.length-1 ? $scope.counter + 1 : 0;
        		  $scope.model.dataProviderID = $scope.model.valuelistID[$scope.counter].realValue
        	  }
        	  $scope.svyServoyapi.apply('dataProviderID')
          }
          
          $scope.decrement = function()
          {
        	  if ($scope.model.valuelistID)
        	  {
        		  $scope.counter = $scope.counter > 0 ? $scope.counter - 1 : $scope.model.valuelistID.length-1;
        		  $scope.model.dataProviderID = $scope.model.valuelistID[$scope.counter].realValue
        	  }
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
           * Request the focus to this spinner.
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
          
          function getSelectionFromDataprovider()
          {
        	  if(!$scope.model.dataProviderID) 
        	  {
        		$scope.counter = 0;
        		return undefined
        	  }
              
	          for(var i = 0; i < $scope.model.valuelistID.length; i++)
	          {
	          	  var item = $scope.model.valuelistID[i];
	          	  if(item && item.realValue && $scope.model.dataProviderID == item.realValue)
	          	  {
	          		  var displayFormat = undefined;
	          		  var type = undefined;
	          		  if($scope.model.format && $scope.model.format.display) displayFormat = $scope.model.format.display;
	          		  if($scope.model.format && $scope.model.format.type) type = $scope.model.format.type;
	          		  $scope.counter = i;
	          		  return formatFilter(item.displayValue, displayFormat ,type);
	          	  }
	          }
          }
      },
      templateUrl: 'servoydefault/spinner/spinner.html'
    };
}])
