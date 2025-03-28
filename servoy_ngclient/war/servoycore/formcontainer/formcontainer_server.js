var isShowing = false;


$scope.setters.setContainedForm = function(form) {
	if (isShowing) {
		if ($scope.model.containedForm && !servoyApi.hideForm($scope.model.containedForm)) {
			return false;
		}

		if (!servoyApi.showForm(form, $scope.model.relationName)) {
			return false;
		}
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