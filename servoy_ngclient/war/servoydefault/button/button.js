servoyModule.directive('svyButton', function($servoy) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'servoydefault/button/button.html',
      replace: true
    };
  })

  
  
  
  
