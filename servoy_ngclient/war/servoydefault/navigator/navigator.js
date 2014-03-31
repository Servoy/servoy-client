angular.module('svyNavigator',['servoy','ui.slider']).directive('svyNavigator', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      controller: function($scope) {  
    	  
    	  $scope.editIndex = 0;
    	  $scope.sliderIndex = 0;
    	  $scope.$watch('model.currentIndex', function (newVal, oldVal, scope) {
    		  if(!newVal) return;
    		  $scope.sliderIndex = -1*newVal
    		  $scope.editIndex = newVal
    	  })
    	  
    	  $scope.setIndex =  function (idx){
    		  var i = parseInt(idx)
    		  $scope.handlers.setSelectedIndex(window.Math.abs(i));
    	  }
    	  
    	  $scope.sliderStop = function(event, ui) {
    		  $scope.setIndex($scope.sliderIndex)
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