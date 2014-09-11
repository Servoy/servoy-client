angular.module('menubar',['servoy']).directive('menubar', function() {  
    return {
      restrict: 'E',
      transclude: true,
      controller: function($scope, $element, $attrs) {
    	 
      },
      templateUrl: 'canvascomponents/menubar/menubar.html',
      replace: true
    };
  })