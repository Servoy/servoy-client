angular.module('bootstrapcomponentsProgressbar',['servoy'])
.directive('bootstrapcomponentsProgressbar',[function() {  
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
  