servoyModule.directive('svyListbox', function($servoy,$utils,$parse) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      require: 'ngModel',
      compile: function(tElement, tAttrs) {
    	  if ($parse(tAttrs.svyModel)(angular.element(tElement).scope()).multiselectListbox != true)
    	  {
    		  tElement.removeAttr("multiple");
    	  }
    	  return function($scope, $element, $attrs, ngModel) {
    		  $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    		  $utils.watchProperty($scope,'model.background',$scope.style,'backgroundColor')
    		  $utils.watchProperty($scope,'model.foreground',$scope.style,'color')
    		  $scope.$watch('model.dataProviderID', function() {
    			  if(!$scope.model.dataProviderID)
    			  {
    				  $scope.convertModel = null;
    			  }
    			  else
    			  {
    				  $scope.convertModel = $scope.model.dataProviderID.split('\n'); 
    			  }
    		  })
    		  $scope.$watch('convertModel', function() {
    			  var oldValue = $scope.model.dataProviderID;
    			  var newValue = null;
    			  if(!$scope.convertModel)
    			  {
    				  newValue = null;
    			  }
    			  else
    			  {
    				  newValue = $scope.convertModel.join('\n'); 
    			  }
    			  if (oldValue != newValue)
    			  {
    				  $scope.model.dataProviderID = newValue;
    				  $scope.handlers.svy_apply('dataProviderID');
    			  }	  
    		  })
    	  } 
      },
      templateUrl: 'servoydefault/listbox/listbox.html',
      replace: true
    };
  })

  
  
  
  
