angular.module('servoycomponentsColorthefoundset',['servoy']).directive('servoycomponentsColorthefoundset', function($timeout) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
    	  model: "=svyModel",
    	  svyApply: '='
      },
      link: function($scope, $element, $attrs) {
         $scope.$watch('model.foundsetToShow.serverSize', function (newValue) {
        	 var wanted = Math.min(newValue, 10);
        	 if (wanted > $scope.model.foundsetToShow.viewPort.size) $scope.model.foundsetToShow.loadRecordsAsync(0, wanted);
         });
         $scope.$watch('model.foundsetToShow.viewPort.size', function (newValue) {
        	 if (newValue == 0 && $scope.model.foundsetToShow.serverSize > 0) {
        		 // this foundset was updated completely (for example in case of a related foundset the parent record changed thus foundset contents completely changed)
        		 // in which case server automatically sets viewport size/start index to 0
        		 var wanted = Math.min($scope.model.foundsetToShow.serverSize, 10);
        		 $scope.model.foundsetToShow.loadRecordsAsync(0, wanted);
        	 }
         });
      },
      templateUrl: 'servoycomponents/colorthefoundset/colorthefoundset.html',
      replace: true
    };
});