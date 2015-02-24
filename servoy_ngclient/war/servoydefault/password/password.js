angular.module('servoydefaultPassword',['servoy']).directive('servoydefaultPassword', function() {  
    return {
      restrict: 'E',
      scope: {
        model: "=svyModel",
        api: "=svyApi",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
          $scope.findMode = false;
    	 // fill in the api defined in the spec file
    	 $scope.api.onDataChangeCallback = function(event, returnval) {
    		 if(!returnval) {
    			 $element[0].childNodes[0].focus();
    		 }
    	 },
    	 $scope.api.requestFocus = function() { 
    		  $element[0].childNodes[0].focus()
    	 }
    	 
    	// special method that servoy calls when this component goes into find mode.
      	 $scope.api.setFindMode = function(findMode, editable) {
      		$scope.findMode = findMode;
      	 	if (findMode)
      	 	{
      	 		$scope.wasEditable = $scope.model.editable;
      	 		if (!$scope.model.editable) $scope.model.editable = editable;
      	 	}
      	 	else
      	 	{
      	 		$scope.model.editable = $scope.wasEditable != undefined ? $scope.wasEditable : editable;
      	 	}
      	 }; 
      },
      templateUrl: 'servoydefault/password/password.html'
    };
  })

  
  
  
  
