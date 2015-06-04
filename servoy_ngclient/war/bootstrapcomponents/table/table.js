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
  .filter('showDisplayValue', function () { // filter that takes the realValue as an input and returns the displayValue
	return function (input, valuelist) {
		if (valuelist) {
			for (i = 0; i < valuelist.length; i++) {
				if (input === valuelist[i].realValue) {
					return valuelist[i].displayValue;
				}
			}
		}
		return input;
	};
});

  
  
