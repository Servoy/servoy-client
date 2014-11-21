angular.module('servoydefaultSpinner',['servoy']).directive('servoydefaultSpinner',['formatFilterFilter', function(formatFilter) {  
    return {
      restrict: 'E',
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi",
        svyApply: "="
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
        	  $scope.svyApply('dataProviderID')
          }
          
          $scope.decrement = function()
          {
        	  if ($scope.model.valuelistID)
        	  {
        		  $scope.counter = $scope.counter > 0 ? $scope.counter - 1 : $scope.model.valuelistID.length-1;
        		  $scope.model.dataProviderID = $scope.model.valuelistID[$scope.counter].realValue
        	  }
        	  $scope.svyApply('dataProviderID')
          }
          
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
          
       // special method that servoy calls when this component goes into find mode.
       	 $scope.api.setFindMode = function(findMode, editable) {
       		$scope.findMode = findMode;
       	 	if (findMode)
       	 	{
       	 		$scope.wasEditable = $scope.model.editable;
       	 		if (!$scope.model.editable) $scope.model.editable = editable;
       	 	}
       	 	else
       	 	{
       	 		$scope.model.editable = $scope.wasEditable != undefined ? $scope.wasEditable : editable;
       	 	}
       	 };
      },
      templateUrl: 'servoydefault/spinner/spinner.html'
    };
}])
