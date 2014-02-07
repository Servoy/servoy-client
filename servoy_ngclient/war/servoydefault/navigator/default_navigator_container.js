servoyModule.controller('DefaultNavigatorController', function ($scope, $servoy , $solutionSettings){
	
	$scope.default_navi = {};
		
	//TODO treat multiple windows with default navigators
	$scope.$watch('$solutionSettings.mainForm', function (newVal, oldVal, scope) {
		    if($solutionSettings.mainForm.name) {
		    	var name = $solutionSettings.mainForm.name
		    	$servoy.getFormState(name).then(function(formState){
			    	$scope.default_navi.model = formState.model.svy_default_navigator;
			    	$scope.default_navi.handlers = formState.handlers.svy_default_navigator;
		    	})
		    }
	});
	
})