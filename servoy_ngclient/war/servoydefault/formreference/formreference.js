angular.module('servoydefaultFormreference',['servoy']).directive('servoydefaultFormreference', function() {  
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      },
      link: function($scope, $element, $attrs) {
       
      },
      templateUrl: 'servoydefault/formreference/formreference.html'
 };
})