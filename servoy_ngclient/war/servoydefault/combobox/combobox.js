angular.module('svyCombobox',['servoy']).directive('svyCombobox', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel"
      },
      controller: function($scope, $element, $attrs) {
    	   $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	   $scope.customClasses = "";
    	   // uncomment the following comment to use select2 as default
    	   $scope.isSelect2 = ($scope.model.styleClass && ($scope.model.styleClass.indexOf('select2', 0) == 0)) /* || (typeof $scope.model.styleClass == 'undefined')*/;
      },
      link: function(scope, element, attr) {
    	  // see http://ivaynberg.github.io/select2/ for what this component allows (also can do typeahead, multi-edit field and so on)
    	  // we could somehow give to select2() method 'containerCssClass' and 'dropdownCssClass' as well if needed in the future (for more custom styling)
    	  
    	  if (scope.model.styleClass && scope.model.styleClass.indexOf('select2 ', 0) == 0) {
    		  // transform it into a select2 bootstrap combo and append styles
    		  $(element).children("select").select2({
    			  containerCss: scope.style,
    			  containerCssClass: scope.model.styleClass.substr(8, scope.model.styleClass.length - 8)
    		  });
    	  } else if (scope.isSelect2) {
    		  // transform it into a default select2 bootstrap combo
    		  $(element).children("select").select2({
    			  containerCss: scope.style
    		  });
    	  } else scope.customClasses = (scope.model.styleClass ? scope.model.styleClass : "form-control input-sm");
      },
      templateUrl: 'servoydefault/combobox/combobox.html',
      replace: true
    };
  })

  
  
  
  
