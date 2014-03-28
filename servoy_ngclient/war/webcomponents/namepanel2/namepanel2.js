angular.module('namepanel2',['servoy']).directive('namepanel2', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'webcomponents/namepanel2/namepanel2.html',
      replace: true
    };
  })