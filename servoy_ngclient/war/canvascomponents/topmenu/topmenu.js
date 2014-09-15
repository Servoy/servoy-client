angular.module('topmenu',['servoy']).directive('topmenu', function() {
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'canvascomponents/topmenu/topmenu.html',
      replace: true
    };
  })