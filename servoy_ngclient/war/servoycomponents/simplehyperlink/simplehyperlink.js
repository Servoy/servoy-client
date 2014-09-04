angular.module('simplehyperlink',['servoy']).directive('simplehyperlink', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'servoycomponents/simplehyperlink/simplehyperlink.html',
      replace: true
    };
  })