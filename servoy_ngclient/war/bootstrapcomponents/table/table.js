angular.module('bootstrapcomponentsTable',['servoy']).directive('bootstrapcomponentsTable', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
    	  $scope.$watch('model.foundset.serverSize', function (newValue) {
         	 $scope.model.foundset.loadRecordsAsync(0, newValue);
          });
      },
      templateUrl: 'bootstrapcomponents/table/table.html'
    };
  })
  
  
  
