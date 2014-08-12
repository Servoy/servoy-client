angular.module('svyTextarea',['servoy']).directive('svyTextarea', function($apifunctions) {  
    return {
      restrict: 'E',
      transclude: true,
      require: 'ngModel',
      scope: {
        model: "=svyModel",
        api: "=svyApi",
        handlers: "=svyHandlers"
      },
      link:function($scope, $element, $attrs, ngModel) {
        $scope.style = {width:'100%',height:'100%', resize:'none'}
        $scope.findMode = false;
    	  
        var storedTooltip = false;
    	 // fill in the api defined in the spec file
		$scope.api.onDataChangeCallback = function(event, returnval) 
		{
			var stringValue = typeof returnval == 'string'
			if(!returnval || stringValue) {
				$element[0].focus();
				ngModel.$setValidity("", false);
				if (stringValue) {
					if ( storedTooltip == false)
						storedTooltip = $scope.model.toolTipText;
					$scope.model.toolTipText = returnval;
				}
			}
			else {
				ngModel.$setValidity("", true);
				$scope.model.toolTipText = storedTooltip;
				storedTooltip = false;
			}
		}
    	 $scope.api.requestFocus = function() { 
    		  $element[0].focus()
    	 }


         $scope.api.setScroll = function(x, y) {
        	 $element.scrollLeft(x);
        	 $element.scrollTop(y);
         }
         
         $scope.api.getScrollX = function() {
        	 return $element.scrollLeft();
         }
         
         $scope.api.getScrollY = function() {
        	 return $element.scrollTop();
         }
         
    	 $scope.api.getSelectedText = $apifunctions.getSelectedText($element[0]);
    	 $scope.api.setSelection = $apifunctions.setSelection($element[0]);
    	 $scope.api.replaceSelectedText = $apifunctions.replaceSelectedText($element[0]);
    	 $scope.api.selectAll = $apifunctions.selectAll($element[0]);
         
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
      	 		$scope.model.editable = $scope.wasEditable;
      	 	}
      	 };      
      },
      templateUrl: 'servoydefault/textarea/textarea.html',
      replace: true
    };
  })

  
  
  
  
