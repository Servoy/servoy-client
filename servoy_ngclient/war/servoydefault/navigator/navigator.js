angular.module('svyNavigator',['servoy','slider']).directive('svyNavigator', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      controller: function($scope)
      {
    	  $scope.slider_model = {};
    	  $scope.slider_handlers = {};
    	  $scope.slider_api = {};
    	  
    	  $scope.setIndex =  function (idx){
    		  var i = parseInt(idx)
    		  if (!i) i = 1;
    		  $scope.handlers.setSelectedIndex(window.Math.abs(i));
    	  }

    	  $scope.slider_handlers.svyApply = $scope.handlers.svy_apply;
    	  $scope.slider_handlers.onStopMethodID = function(event, value) {
    		  $scope.setIndex(value);
    	  };    	  
		  $scope.slider_model.animate = false;
    	  $scope.slider_model.step = 1;	    		  
    	  $scope.slider_model.orientation = 'vertical';
    	  $scope.slider_model.range = 'max';
		  var model = $scope.model;
		  if (model.maxIndex > 0) {
	   	    $scope.slider_model.min = -1*model.maxIndex;
	        $scope.slider_model.max = -1;
	      }
    	  if (model.currentIndex) {
    	    $scope.slider_model.dataProviderID = -1*model.currentIndex;
    	  }
    	  $scope.$watch('model.maxIndex', function (newVal, oldVal, scope) 
    	  {
	    	$scope.slider_model.min = model.maxIndex > 0? -1*model.maxIndex:0;
	        $scope.slider_model.max = model.maxIndex > 0? -1:0;
    	  });
    	  $scope.$watch('model.currentIndex', function (newVal, oldVal, scope) {
    		  if(!newVal) return;
    	      $scope.slider_model.dataProviderID = -1*newVal;
    	  })    	  
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