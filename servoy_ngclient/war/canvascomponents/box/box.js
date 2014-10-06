angular.module('canvascomponentsBox',['servoy']).directive('canvascomponentsBox', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'canvascomponents/box/box.html',
      replace: true
    };
  })