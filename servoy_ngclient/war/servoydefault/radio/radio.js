servoyModule.directive('svyRadio', function($utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%'}  
          
          $scope.radioClicked = function()
          {
        	  $scope.model.dataProviderID = $scope.model.valuelistID[0].realValue;
        	  $scope.handlers.svy_apply('dataProviderID')
          }
      },
      templateUrl: 'servoydefault/radio/radio.html',
      replace: true
    };
})