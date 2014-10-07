angular.module('canvascomponentsLogo',['servoy']).directive('canvascomponentsLogo', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'canvascomponents/logo/logo.html'
    };
  })