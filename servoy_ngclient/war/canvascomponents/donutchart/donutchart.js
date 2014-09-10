angular.module('donutchart',['servoy']).directive('donutchart', function($window) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
    	  model: "=svyModel",
      },      
      controller: function($scope, $element, $attrs) {
    	  $scope.$watch('model.dataProviderID', function(newVal, oldVal) {
    		  if($scope.model.dataProviderID) {
	    		  Morris.Donut({
	    			  element: 'donut-chart',
	        		  data: $scope.model.dataProviderID.data,
	        		  colors: $scope.model.dataProviderID.colors,  
	        		  resize: false
	    		  });
    		  }
    	  }, true);
    	  
    	  $scope.$watch('elementDimensions', function (newValue, oldValue) {
    		  if($scope.model.dataProviderID) {
	    		  Morris.Donut({
	    			  element: 'donut-chart',
	        		  data: $scope.model.dataProviderID.data,
	        		  colors: $scope.model.dataProviderID.colors,  
	        		  resize: false
	    		  });
    		  }
    	  });

    	  $scope.elementDimensions = { 'h': $element.height(), 'w': $element.width() };
    	  
    	  angular.element($window).bind('resize', function () {
    		  	$scope.elementDimensions = { 'h': $element.height(), 'w': $element.width() };
    		    $scope.$digest();
    	  });
      },
      templateUrl: 'canvascomponents/donutchart/donutchart.html',
      replace: true
    };
  })