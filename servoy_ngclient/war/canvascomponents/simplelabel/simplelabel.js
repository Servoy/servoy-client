angular.module('simplelabel',['servoy']).directive('simplelabel', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'canvascomponents/simplelabel/simplelabel.html',
      replace: true
    };
  })