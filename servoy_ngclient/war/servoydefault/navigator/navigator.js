angular.module('svyNavigator',['servoy']).directive('svyNavigator', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {  
    	  
    	  $scope.editIndex = 0;
    	  $scope.$watch('model.currentIndex', function (newVal, oldVal, scope) {
    		  if(!newVal) return;
    		  $scope.editIndex = newVal
    	  })
    	  
    	  $scope.setIndex =  function (idx){
    		  var i = parseInt(idx)
    		  $scope.handlers.setSelectedIndex(i);	
    	  }
    	  
      },
      templateUrl: 'servoydefault/navigator/navigator.html',
      replace: true
      
    };
}).controller('DefaultNavigatorController', function ($scope, $servoyInternal , $solutionSettings){  // special case using internal api
	
	$scope.default_navi = {};
		
	//TODO treat multiple windows with default navigators
	$scope.$watch('$solutionSettings.mainForm', function (newVal, oldVal, scope) {
		    if($solutionSettings.mainForm.name) {
		    	var name = $solutionSettings.mainForm.name
		    	$servoyInternal.getFormState(name).then(function(formState){
			    	$scope.default_navi.model = formState.model.svy_default_navigator;
			    	$scope.default_navi.handlers = formState.handlers.svy_default_navigator;
		    	})
		    }
	});
	
})