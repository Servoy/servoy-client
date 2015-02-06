angular.module('bootstrapcomponentsSelect',['servoy']).directive('bootstrapcomponentsSelect', function() {  
    return {
      restrict: 'E',
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {

         
      },
      templateUrl: 'bootstrapcomponents/select/select.html'
    };
})