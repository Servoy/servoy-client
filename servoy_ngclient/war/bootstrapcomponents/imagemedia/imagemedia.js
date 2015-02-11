angular.module('bootstrapcomponentsImagemedia',['servoy']).directive('bootstrapcomponentsImagemedia', function($window, $document) {  
    return {
      restrict: 'E',
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {  

      },
      templateUrl: 'bootstrapcomponents/imagemedia/imagemedia.html'
    };
  })