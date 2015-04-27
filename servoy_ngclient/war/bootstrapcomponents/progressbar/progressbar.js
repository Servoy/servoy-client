angular.module('bootstrapcomponentsProgressbar',['servoy'])
.directive('bootstrapcomponentsProgressbar',['$animate', function($animate) {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'bootstrapcomponents/progressbar/progressbar.html'
    };
  }])
  