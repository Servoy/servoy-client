angular.module('bootstrapcomponentsTextbox',['servoy']).directive('bootstrapcomponentsTextbox', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	  
      },
      templateUrl: 'bootstrapcomponents/textbox/textbox.html'
    };
  })
  
  
  
