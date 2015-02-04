angular.module('bootstrapcomponentsLabel',['servoy']).directive('bootstrapcomponentsLabel', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	  
      },
      templateUrl: 'bootstrapcomponents/label/label.html'
    };
  })
  
  
  
