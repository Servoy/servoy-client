angular.module('servoycoreErrorbean',['servoy']).directive('servoycoreErrorbean', function(formatFilterFilter) {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'servoycore/errorbean/errorbean.html'
    };
  })
  
  
  
