angular.module('namepanel',['servoy']).directive('namepanel', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'webcomponents/namepanel/namepanel.html',
      replace: true
    };
  })