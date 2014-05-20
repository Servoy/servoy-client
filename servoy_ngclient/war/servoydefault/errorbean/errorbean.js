angular.module('svyErrorbean',['servoy']).directive('svyErrorbean', function(formatFilterFilter) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
       	model: "=svyModel",
      },
      controller: function($scope, $element, $attrs) {
    	  
      },
      templateUrl: 'servoydefault/errorbean/errorbean.html',
      replace: true
    };
  })
  
  
  
