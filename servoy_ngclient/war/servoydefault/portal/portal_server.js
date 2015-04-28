$scope.api.deleteRecord = function(){
	$scope.model.relatedFoundset.foundset.deleteRecord();
}

$scope.api.duplicateRecord = function(addOnTop){
	if (addOnTop === undefined)
	{
		addOnTop = true;
	}	
	$scope.model.relatedFoundset.foundset.duplicateRecord($scope.api.getSelectedIndex(),addOnTop,true);
}


$scope.api.getMaxRecordIndex = function(){
	return $scope.model.relatedFoundset.foundset.getSize();
}

$scope.api.getScrollX = function(){
	//not implemented for web
	return 0;
}

$scope.api.getScrollY = function(){
	//not implemented for web
	return 0;
}

$scope.api.getSelectedIndex = function(){
	return $scope.model.relatedFoundset.foundset.getSelectedIndex();
}

$scope.api.getSortColumns = function(){
	return $scope.model.relatedFoundset.foundset.getCurrentSort();
}

$scope.api.newRecord = function(addOnTop){
	if (addOnTop === undefined)
	{
		addOnTop = true;
	}	
	$scope.model.relatedFoundset.foundset.newRecord(addOnTop,true);
}

$scope.api.setScroll = function(x,y){
	//not implemented for web
}

$scope.api.setSelectedIndex = function(index){
	$scope.model.relatedFoundset.foundset.setSelectedIndex(index);
}
