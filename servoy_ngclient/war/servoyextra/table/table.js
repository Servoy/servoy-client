angular.module('servoyextraTable',['servoy']).directive('servoyextraTable', ["$timeout",function($timeout) {  
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
    				  if ($scope.model.pageSize * ($scope.model.currentPage -1) > newValue)
    				  {
    					  $scope.model.currentPage =  Math.floor(newValue / $scope.model.pageSize) + 1;
    				  }
    				  else
    				  {
    					  $scope.model.foundset.loadRecordsAsync($scope.model.pageSize * ($scope.model.currentPage -1), $scope.model.pageSize);
    				  }
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
    	  var toBottom = false;
    	  var tbody = null;
    	  var wrapper = null;
    	  $scope.$watch('model.visible', function(newValue) {
    		  if (!newValue) {
    			   toBottom = false;
    			   tbody = null;
    			   wrapper = null;
    		  }
    	  })
    	  function scrollIntoView() {
    		  var firstSelected = $scope.model.foundset.selectedRowIndexes[0];
    		  firstSelected = firstSelected - ($scope.model.pageSize * ($scope.model.currentPage -1));
    		  var child = null;
    		  if (firstSelected == 0) {
    			  child= $element.find("thead");
    		  } 
    		  else child = tbody.children().eq(firstSelected)
			  if (child.length > 0) {
				  var wrapperRect = wrapper.getBoundingClientRect();
				  var childRect =child[0].getBoundingClientRect();
				  if (childRect.top < wrapperRect.top || childRect.bottom > wrapperRect.bottom) {
					  child[0].scrollIntoView(!toBottom);
				  }
			  }
    	  }
    	  $scope.$watch('model.foundset.selectedRowIndexes', function (newValue,oldValue) {
    		  if ( $scope.model.foundset.selectedRowIndexes.length > 0) {
    			  if (tbody == null || tbody.length == 0) {
    				  wrapper = $element.find(".tablewrapper")[0];
    				  tbody= $element.find("tbody");
    			  }
    			  if(tbody.children().length > 1) {
    				  scrollIntoView();
    			  }
    			  else {
    				  $timeout(scrollIntoView, 200)
    			  }
    			 
    		  }
    	  },true)
    	  
    	  $scope.getUrl = function(column,row) {
    		 if (column && row)
    		 {
    			 var index = $scope.model.foundset.viewPort.rows.indexOf(row)
    			if (index >= 0 && column.dataprovider && column.dataprovider[index] && column.dataprovider[index].url)
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
    	  
    	  $scope.tableClicked = function(event, type) {
    		 var elements = document.querySelectorAll(':hover');
    		 for(var i=elements.length;--i>0;) {
    			 var row_column = $(elements[i]).data("row_column");
    			 if (row_column) {
    				 var rowIndex = $scope.model.foundset.viewPort.rows.indexOf(row_column.row); 
    				 var columnIndex = $scope.model.columns.indexOf(row_column.column);
    				 var realRow = $scope.getRealRow(rowIndex);
    				 $scope.model.foundset.selectedRowIndexes = [realRow];
    				 if (type == 1 && $scope.handlers.onCellClick) {
    					 $scope.handlers.onCellClick(realRow + 1, columnIndex, $scope.model.foundset.viewPort.rows[rowIndex]);
    		    	 }
    		    	  
    		    	 if ( type == 2 && $scope.handlers.onCellRightClick) {
    		    		 $scope.handlers.onCellRightClick(realRow + 1, columnIndex, $scope.model.foundset.viewPort.rows[rowIndex]);
    		    	 }
    			 }
    		 }
    	  }
    	  if ($scope.handlers.onCellRightClick) {
    		  $scope.tableRightClick = function(event) {
    			  $scope.tableClicked(event,2);
    		  }
    	  }
    	  
    	  if ($scope.handlers.onHeaderClick) {
    		  $scope.headerClicked = function(column) {
    			  $scope.handlers.onHeaderClick(column);
    		  }
    	  }
    	  

    	  $scope.getRowStyle = function(row) {
    		  var isSelected = $scope.model.foundset.selectedRowIndexes && $scope.model.foundset.selectedRowIndexes.indexOf($scope.getRealRow(row)) != -1; 
    		  return  isSelected ? $scope.model.selectionClass : " ";
    	  }
    	  
    	  $scope.keyPressed = function(event) {
    		  var fs = $scope.model.foundset;
    		  if (fs.selectedRowIndexes && fs.selectedRowIndexes.length > 0) {
    			  var selection = fs.selectedRowIndexes[0];
	    		  if (event.keyCode == 38) {
	    			  if (selection > 0) {
	    				  fs.selectedRowIndexes = [selection-1];
	    				  if ( (fs.viewPort.startIndex) <=  selection-1){
	    					  toBottom = false;
	    				  }
	    				  else $scope.modifyPage(-1);
	    			  }
	    			  event.preventDefault();
	    		  }
	    		  else if (event.keyCode == 40) {
	    			  if (selection < fs.serverSize-1) {
	    				  fs.selectedRowIndexes = [selection+1];
	    				  if ( (fs.viewPort.startIndex + fs.viewPort.size) >  selection+1){
	    					  toBottom = true;
	    				  }
	    				  else $scope.modifyPage(1);
	    			  }
	    			  event.preventDefault();
	    		  } 
	    		  else if (event.keyCode == 13) {
	    			 if ($scope.handlers.onCellClick) {
	    				 $scope.handlers.onCellClick(selection+1, null,fs.viewPort.rows[selection])
	    			 }
	    		  }
    		  }
    	  }
      },
      templateUrl: 'servoyextra/table/table.html'
    };
  }])
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
}).directive('modelInData', function($parse) {
	   return {
		     restrict: 'A',
		     link: function($scope, $element, $attrs) {
		       var model = $parse($attrs.modelInData)($scope);
		       $element.data('row_column', model);
		     }
		   }
});

  
  
