angular.module('svyTextarea',['servoy']).directive('svyTextarea', function($apifunctions) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        api: "=svyApi",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%', resize:'none'}
          
    	  
    	 // fill in the api defined in the spec file
    	 $scope.api.onDataChangeCallback = function(event, returnval) {
    		 if(!returnval) {
    			 $element[0].focus();
    		 }
    	 },
    	 $scope.api.requestFocus = function() { 
    		  $element[0].focus()
    	 },
    	 $scope.api.getSelectedText = $apifunctions.getSelectedText($element[0]);
    	 $scope.api.setSelection = $apifunctions.setSelection($element[0]);
      },
      templateUrl: 'servoydefault/textarea/textarea.html',
      replace: true
    };
  })

  
  
  
  
