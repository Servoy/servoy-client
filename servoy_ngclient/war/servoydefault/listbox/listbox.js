servoyModule.directive('svyListbox', function($servoy,$utils,$parse) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel"
      },
      require: 'ngModel',
      link:  function($scope, $element, $attrs, ngModel) {
    	   $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
           $utils.watchProperty($scope,'model.background',$scope.style,'backgroundColor')
           $utils.watchProperty($scope,'model.foreground',$scope.style,'color')
           if ($scope.model.multiselectListbox && $scope.model.multiselectListbox == true)
           {
        	   ngModel.$formatters.push(function(modelValue) {
        		   // angular model is an array
        		   return modelValue.split('\n'); //converted
        	   });
           }
      },
      templateUrl: function (tElement, tAttrs) {
    	  if (tAttrs.multiple == true)
    	  {
    		  return 'servoydefault/listbox/multiselectlistbox.html';
    	  }
    	  else
    	  {
    		  return 'servoydefault/listbox/listbox.html';
    	  }
      },
      replace: true
    };
  })

  
  
  
  
