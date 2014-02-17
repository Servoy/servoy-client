servoyModule.directive('svyLabel', ["formatFilterFilter",'$utils', function(formatFilter,$utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
      	model: "=svyModel"
      },
      controller: function($scope, $element, $attrs) {
       $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
       $utils.watchProperty($scope,'model.background',$scope.style,'backgroundColor')
       $utils.watchProperty($scope,'model.foreground',$scope.style,'color')
       
       $scope.$watch('model.imageMediaID', function() {
			 if ($scope.model.imageMediaID) {
			    $scope.bgstyle = {width:'100%',height:'100%',overflow:'hidden'};
       			$scope.bgstyle['background-image'] = "url('" + $scope.model.imageMediaID + "')"; 
				$scope.bgstyle['background-repeat'] = "no-repeat";
				$scope.bgstyle['background-position'] = "left";
				$scope.bgstyle['background-size'] = "contain";
				$scope.bgstyle['display'] = "inline-block";
    			$scope.bgstyle['vertical-align'] = "middle"; 
       		 }
       		 else {
       		 	delete $scope.bgstyle;
       		 }
       })
      },
      templateUrl: 'servoydefault/label/label.html',
      replace: true
    };
}])