servoyModule.directive('svyTextarea', function($servoy,$utils,$apifunctions) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%', resize:'none'}
          angular.extend($scope.style ,$utils.getScrollbarsStyleObj($scope.model.scrollbars));
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
    	 $scope.api.getSelectedText = $apifunctions.getSelectedText($element[0]);
    	 $scope.api.setSelection = $apifunctions.setSelection($element[0]);
      },
      templateUrl: 'servoydefault/textarea/textarea.html',
      replace: true
    };
  })

  
  
  
  
