var isShowing = false;


$scope.setters.setContainedForm = function(form) {
	if (isShowing) {
		if ($scope.model.containedForm && !servoyApi.hideForm($scope.model.containedForm)) {
			return false;
		}
		//wait until form and relation are both correctly set
		servoyApi.showFormDelayed($scope.model, 'containedForm', 'relationName');
	}
	$scope.model.containedForm = form;
}

/**
* Servoy component lifecycle callback
*/
$scope.onShow = function() {
	isShowing = true;
	if ($scope.model.containedForm) {
		servoyApi.showForm($scope.model.containedForm, $scope.model.relationName);
	}
}

/**
* Servoy component lifecycle callback
*/
$scope.onHide = function() {
	isShowing = false;
}