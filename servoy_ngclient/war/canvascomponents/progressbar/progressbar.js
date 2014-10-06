angular.module('canvascomponentsCanvasProgressbar',['servoy']).directive('canvascomponentsCanvasProgressbar', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'canvascomponents/progressbar/progressbar.html',
      replace: true
    };
  })