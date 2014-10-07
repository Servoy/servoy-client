angular.module('canvascomponentsSimplelabel',['servoy']).directive('canvascomponentsSimplelabel', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'canvascomponents/simplelabel/simplelabel.html'
    };
  })