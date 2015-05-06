angular.module('servoydefaultSpinner',['servoy']).directive('servoydefaultSpinner',['formatFilterFilter','$apifunctions', function(formatFilter, $apifunctions) {  
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
          
    	  $scope.api.getWidth = $apifunctions.getWidth($element[0]);
    	  $scope.api.getHeight = $apifunctions.getHeight($element[0]);
    	  $scope.api.getLocationX = $apifunctions.getX($element[0]);
    	  $scope.api.getLocationY = $apifunctions.getY($element[0]);
      },
      templateUrl: 'servoydefault/spinner/spinner.html'
    };
}])
