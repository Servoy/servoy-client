angular.module('servoycomponentsNamepanel2',['servoy']).directive('servoycomponentsNamepanel2', function() {  
    return {
      restrict: 'E',
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'servoycomponents/namepanel2/namepanel2.html',
      replace: true
    };
  })