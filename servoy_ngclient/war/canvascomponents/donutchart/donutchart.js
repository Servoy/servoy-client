angular.module('canvascomponentsDonutchart',['servoy']).directive('canvascomponentsDonutchart', function($window) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
    	  model: "=svyModel",
      },      
      controller: function($scope, $element, $attrs) {
    	  $scope.formatter = function(value, data) {
    		  var total = 0;
    		  for(var e in $scope.model.dataProviderID.data) {
    			  total += $scope.model.dataProviderID.data[e].value;
    		  }
    		  return (value * 100 / total).toFixed(0) + '%';
    	  };
    	  
    	  $scope.$watch('model.dataProviderID', function(newVal, oldVal) {
    		  if($scope.model.dataProviderID) {
	    		  Morris.Donut({
	    			  element: 'donut-chart',
	        		  data: $scope.model.dataProviderID.data,
	        		  colors: $scope.model.dataProviderID.colors,  
	        		  resize: false,
	        		  formatter: $scope.formatter
	    		  });
    		  }
    	  }, true);
    	  
    	  $scope.$watch('elementDimensions', function (newValue, oldValue) {
    		  if($scope.model.dataProviderID) {
	    		  Morris.Donut({
	    			  element: 'donut-chart',
	        		  data: $scope.model.dataProviderID.data,
	        		  colors: $scope.model.dataProviderID.colors,  
	        		  resize: false,
	        		  formatter: $scope.formatter
	    		  });
    		  }
    	  });

    	  $scope.elementDimensions = { 'h': $element.height(), 'w': $element.width() };
    	  
    	  angular.element($window).bind('resize', function () {
    		  	$scope.elementDimensions = { 'h': $element.height(), 'w': $element.width() };
    		    $scope.$digest();
    	  });
      },
      templateUrl: 'canvascomponents/donutchart/donutchart.html'
    };
  })