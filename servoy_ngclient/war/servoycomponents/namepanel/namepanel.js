angular.module('servoycomponentsNamepanel',['servoy']).directive('servoycomponentsNamepanel', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'servoycomponents/namepanel/namepanel.html',
      replace: true
    };
  })