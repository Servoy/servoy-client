servoyModule.directive('svyLabel', ["formatFilterFilter", function(formatFilter) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
      	model: "=svyModel"
      },
      controller: function($scope, $element, $attrs) {
       $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
       $scope.$watch('model.background', function() {
			 if ($scope.model.background) {
       			$scope.style['background-color'] = $scope.model.background;
       		 }
       		 else {
       		 	delete $scope.style['background-color'];
       		 }
       })
      },
      templateUrl: 'servoydefault/label/label.html',
      replace: true
    };
}])