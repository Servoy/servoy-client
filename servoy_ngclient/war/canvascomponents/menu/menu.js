angular.module('canvascomponentsMenu',['servoy']).directive('canvascomponentsMenu', function() {  
    return {
      restrict: 'E',
      transclude: true,
      controller: function($scope, $element, $attrs) {
    	 
      },
      templateUrl: 'canvascomponents/menu/menu.html',
      replace: true
    };
  })