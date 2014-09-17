angular.module('menuitem',['servoy']).directive('menuitem', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'canvascomponents/menuitem/menuitem.html',
      replace: true
    };
  })