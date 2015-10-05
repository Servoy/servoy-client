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
    	  
    	  $scope.rowClicked = function(row) {
    		  $scope.model.foundset.selectedRowIndexes = [row];
    	  }
    	  
    	  $scope.cellClicked = function(row, column) {
			  if ($scope.handlers.onCellClick) {
				  $scope.handlers.onCellClick(row + 1, column + 1);
			  }    		  
    	  }

    	  $scope.getRowStyle = function(row) {
    		  var isSelected = $scope.model.foundset.selectedRowIndexes && $scope.model.foundset.selectedRowIndexes.indexOf(row) != -1; 
    		  return  isSelected ? $scope.model.selectionClass : "";
    	  }
      },
      templateUrl: 'bootstrapcomponents/table/table.html'
    };
  })
  .filter('getDisplayValue', function () { // filter that takes the realValue as an input and returns the displayValue
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

  
  
