angular.module('servoyextraTable',['servoy']).directive('servoyextraTable', function() {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
    	  $scope.$watch('model.foundset.serverSize', function (newValue) {
    		  if (newValue)
    		  {
    			  if (!$scope.showPagination())
    			  {
    				  $scope.model.foundset.loadRecordsAsync(0, newValue);
    			  }
    			  else
    			  {
    				  $scope.model.foundset.loadRecordsAsync($scope.model.pageSize * ($scope.model.currentPage -1), $scope.model.pageSize);
    			  }	  
    		  }	  
          });
    	  
    	  $scope.$watch('model.currentPage', function (newValue) {
    		  if (newValue &&  $scope.showPagination())
    		  {
    			  $scope.model.foundset.loadRecordsAsync($scope.model.pageSize * (newValue -1), $scope.model.pageSize);
    		  }	  
          });
    	  
    	  $scope.$watch('model.pageSize', function (newValue,oldValue) {
    		  if (oldValue && newValue &&  $scope.showPagination())
    		  {
    			  $scope.model.foundset.loadRecordsAsync($scope.model.pageSize * ($scope.model.currentPage -1), $scope.model.pageSize);
    		  }	  
          });
    	  
    	  $scope.getUrl = function(column,row) {
    		 if (column && row)
    		 {
    			 var index = $scope.model.foundset.viewPort.rows.indexOf(row)
    			if (index >= 0 && column.dataprovider[index] && column.dataprovider[index].url)
    			{
    				 return column.dataprovider[index].url;
    			}	 
    		 }	  
       		 return null; 
       	  }
    	  
    	  $scope.hasNext = function() {
      		 return $scope.model.foundset && $scope.model.currentPage < Math.ceil($scope.model.foundset.serverSize / $scope.model.pageSize); 
      	  }
    	  
    	  $scope.showPagination = function() {
     		 return $scope.model.pageSize && $scope.model.foundset && $scope.model.foundset.serverSize > $scope.model.pageSize; 
     	  }
    	  
    	  $scope.modifyPage = function(count) {
    		var pages = Math.ceil($scope.model.foundset.serverSize / $scope.model.pageSize)
    		var newPage = $scope.model.currentPage + count;
    		if (newPage >= 1 && newPage <= pages)
    		{
    			$scope.model.currentPage = newPage;
    		}	
    	  }
    	  
    	  $scope.getRealRow = function(row) {
    		  var realRow = row;
    		  if ($scope.showPagination())
    		  {
    			  realRow = realRow + $scope.model.pageSize * ($scope.model.currentPage -1);
    		  }	
    		  return realRow;
    	  }
    	  
    	  $scope.rowClicked = function(row) {
    		  $scope.model.foundset.selectedRowIndexes = [$scope.getRealRow(row)];
    	  }
    	  
    	  if ($scope.handlers.onHeaderClick) {
    		  $scope.headerClicked = function(column) {
    			  $scope.handlers.onHeaderClick(column + 1);
    		  }
    	  }
    	  
    	  if ($scope.handlers.onCellClick) {
    		  $scope.cellClicked = function(row, column) {
    			  $scope.handlers.onCellClick($scope.getRealRow(row) + 1, column + 1);
    		  }
    	  }

    	  $scope.getRowStyle = function(row) {
    		  var isSelected = $scope.model.foundset.selectedRowIndexes && $scope.model.foundset.selectedRowIndexes.indexOf($scope.getRealRow(row)) != -1; 
    		  return  isSelected ? $scope.model.selectionClass : "";
    	  }
      },
      templateUrl: 'servoyextra/table/table.html'
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

  
  
