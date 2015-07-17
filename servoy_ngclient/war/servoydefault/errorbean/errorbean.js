angular.module('servoydefaultErrorbean',['servoy']).directive('servoydefaultErrorbean', function(formatFilterFilter) {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'servoydefault/errorbean/errorbean.html'
    };
  })
  
  
  
