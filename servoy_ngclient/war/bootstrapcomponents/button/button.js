angular.module('bootstrapcomponentsButton',['servoy']).directive('bootstrapcomponentsButton', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	  
      },
      templateUrl: 'bootstrapcomponents/button/button.html'
    };
  })
  
  
  
