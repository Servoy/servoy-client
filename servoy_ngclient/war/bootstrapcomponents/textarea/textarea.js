angular.module('bootstrapcomponentsTextarea',['servoy']).directive('bootstrapcomponentsTextarea', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	  
      },
      templateUrl: 'bootstrapcomponents/textarea/textarea.html'
    };
  })
  
  
  
