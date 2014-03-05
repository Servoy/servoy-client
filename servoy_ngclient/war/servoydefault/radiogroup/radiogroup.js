servoyModule.directive('svyRadiogroup', function($utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
          $scope.notNull = $utils.notNull // TODO remove the need for this
          $scope.style = {width:'100%',height:'100%'}
          angular.extend($scope.style ,$utils.getScrollbarsStyleObj($scope.model.scrollbars));
          $utils.watchProperty($scope,'model.background',$scope.style,'backgroundColor')
          $utils.watchProperty($scope,'model.foreground',$scope.style,'color')
      },
      templateUrl: 'servoydefault/radiogroup/radiogroup.html',
      replace: true
    };
  })

  
  
  
  
