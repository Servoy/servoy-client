angular.module('svyButton',['servoy']).directive('svyButton', function(formatFilterFilter) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	  $scope.containerstyle = {overflow:'hidden',position:'absolute'}
          $scope.contentstyle = {width:'100%',overflow:'hidden',position:'relative',whiteSpace:'nowrap'}
      },
      templateUrl: 'servoydefault/button/button.html',
      replace: true
    };
  })
  
  
  
