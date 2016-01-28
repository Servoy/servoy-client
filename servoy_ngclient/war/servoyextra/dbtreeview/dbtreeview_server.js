$scope.api.addRoots = function(root) {
	if(!$scope.model.roots) {
		$scope.model.roots = []
	}
	$scope.model.roots.push(root)
}

$scope.api.removeAllRoots = function() {
	if($scope.model.roots) {
		$scope.model.roots.length = 0;
	}
}

$scope.api.setTextDataprovider = function(datasource, textdataprovider) {
	$scope.getBinding(datasource).textdataprovider = textdataprovider;	   	
}

$scope.api.setNRelationName = function(datasource, nrelationname) {
	$scope.getBinding(datasource).nrelationname = nrelationname;
}   

$scope.api.setHasCheckBoxDataprovider = function(datasource, hascheckboxdataprovider) {
	$scope.getBinding(datasource).hascheckboxdataprovider = hascheckboxdataprovider;
}   

$scope.api.setCallBackInfo = function(datasource, callbackfunction, param) {
	$scope.getBinding(datasource).callbackinfo = {f: callbackfunction, param: param }
}   

$scope.api.setCheckBoxValueDataprovider = function(datasource, checkboxvaluedataprovider) {
	$scope.getBinding(datasource).checkboxvaluedataprovider = checkboxvaluedataprovider;
}   

$scope.api.setMethodToCallOnCheckBoxChange = function(datasource, callbackfunction, param) {
	$scope.getBinding(datasource).methodToCallOnCheckBoxChange = {f: callbackfunction, param: param }  		
}

$scope.api.setToolTipTextDataprovider = function(datasource, tooltiptextdataprovider) {
	$scope.getBinding(datasource).tooltiptextdataprovider = tooltiptextdataprovider;
}   

$scope.api.setImageURLDataprovider = function(datasource, imageurldataprovider) {
	$scope.getBinding(datasource).imageurldataprovider = imageurldataprovider;
}   

$scope.api.setChildSortDataprovider = function(datasource, childsortdataprovider) {
	$scope.getBinding(datasource).childsortdataprovider = childsortdataprovider;
}   

$scope.api.setMethodToCallOnDoubleClick = function(datasource, callbackfunction, param) {
	$scope.getBinding(datasource).methodToCallOnDoubleClick = {f: callbackfunction, param: param }  		
}

$scope.getBinding = function(datasource) {
	if(!$scope.model.bindings) {
		$scope.model.bindings = [];
	}

	for(var i = 0; i < $scope.model.bindings.length; i++) {
		if($scope.model.bindings[i].datasource == datasource) {
			return $scope.model.bindings[i];
		}
	}
	
	var lastIdx = $scope.model.bindings.length;
	$scope.model.bindings[lastIdx] = {
			datasource: datasource
	};	
	return $scope.model.bindings[lastIdx];
}

$scope.api.setSelectionPath = function(pk) {
	$scope.model.selection = pk;
}

$scope.api.setNodeLevelVisible = function(level, state) {
	$scope.model.levelVisibility = {level: level, state: state};	
}