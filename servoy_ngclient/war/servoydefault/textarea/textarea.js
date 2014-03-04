servoyModule.directive('svyTextarea', function($servoy,$utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%',overflow:'hidden', resize:'none'}
          $utils.watchProperty($scope,'model.background',$scope.style,'backgroundColor')
          $utils.watchProperty($scope,'model.foreground',$scope.style,'color')
          
    	  
    	 // fill in the api defined in the spec file
    	 $scope.api.onDataChangeCallback = function(event, returnval) {
    		 if(!returnval) {
    			 $element[0].focus();
    		 }
    	 },
    	 $scope.api.requestFocus = function() { 
    		  $element[0].focus()
    	 },
    	 $scope.api.getSelectedText = $utils.getSelectedTextApi($element[0]);
    	 $scope.api.setSelection = $utils.setSelectionApi($element[0]);
      },
      templateUrl: 'servoydefault/textarea/textarea.html',
      replace: true
    };
  })

  
  
  
  
