angular.module('colorthefoundset',['servoy']).directive('colorthefoundset', function($timeout) {  
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
      },
      templateUrl: 'servoycomponents/colorthefoundset/colorthefoundset.html',
      replace: true
    };
});