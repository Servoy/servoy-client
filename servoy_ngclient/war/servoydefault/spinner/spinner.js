angular.module('svySpinner',['servoy']).directive('svySpinner', function($utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
    	  $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	  
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
              $scope.$apply( (isScrollingUp(e)) ? $scope.increment() : $scope.decrement() );
              e.preventDefault();
          });
          input.bind('keydown keypress', function(e){
        	  if (e.which == 40) $scope.decrement();
        	  if (e.which == 38) $scope.increment();
        	  e.preventDefault();
          });
        	  
          $scope.increment = function()
          {
        	  if ($scope.model.valuelistID)
        	  {
        		  $scope.counter = $scope.counter < $scope.model.valuelistID.length-1 ? $scope.counter + 1 : 0;
        		  $scope.model.dataProviderID = $scope.model.valuelistID[$scope.counter].realValue
        	  }
        	  $scope.handlers.svy_apply('dataProviderID')
          }
          
          $scope.decrement = function()
          {
        	  if ($scope.model.valuelistID)
        	  {
        		  $scope.counter = $scope.counter > 0 ? $scope.counter - 1 : $scope.model.valuelistID.length-1;
        		  $scope.model.dataProviderID = $scope.model.valuelistID[$scope.counter].realValue
        	  }
        	  $scope.handlers.svy_apply('dataProviderID')
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
	          	  if(item.realValue && $scope.model.dataProviderID == item.realValue)
	          	  {
	          		  return item.displayValue;
	          		  $scope.counter = i;
	          	  }
	          }
          }
      },
      templateUrl: 'servoydefault/spinner/spinner.html',
      replace: true
    };
})