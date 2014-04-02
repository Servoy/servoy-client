angular.module('svyButton',['servoy']).directive('svyButton', function(formatFilterFilter) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
       $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
       
      },
      templateUrl: 'servoydefault/button/button.html',
      replace: true
    };
  })
  
  
  
