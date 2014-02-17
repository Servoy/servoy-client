servoyModule.directive('svyCombobox', function($servoy,$utils) {  
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
      },
      templateUrl: 'servoydefault/combobox/combobox.html',
      replace: true
    };
  })

  
  
  
  
