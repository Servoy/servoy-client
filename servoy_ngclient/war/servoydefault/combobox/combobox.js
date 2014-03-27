angular.module('svyCombobox',['servoy'])

.directive('svyCombobox', ['$svyNGEvents', function($svyNGEvents) {
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel"
      },
      controller: function($scope, $element, $attrs) {
    	   $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	   $scope.customClasses = "";
    	   // uncomment the following comment to use select2 as default; or the other way around
    	   $scope.isSelect2 = ($scope.model.styleClass && ($scope.model.styleClass.indexOf('select2', 0) == 0)) /**/|| (typeof $scope.model.styleClass == 'undefined')/**/;
      },
      link: function(scope, element, attr) {
    	  // see http://ivaynberg.github.io/select2/ for what this component allows (also can do typeahead, multi-edit field and so on)
    	  // we could somehow give to select2() method 'containerCssClass' and 'dropdownCssClass' as well if needed in the future (for more custom styling)
    	  var select2Css = null;
    	  if (scope.model.styleClass && scope.model.styleClass.indexOf('select2 ', 0) == 0) {
    		  // transform it into a select2 bootstrap combo and append styles
    		  select2Css = "select2-container-svy-xs " + scope.model.styleClass.substr(8, scope.model.styleClass.length - 8);
    	  } else if (scope.isSelect2) {
    		  // transform it into a default select2 bootstrap combo
    		  select2Css = "select2-container-svy-xs";
    	  }
    	  
    	  if (select2Css != null) {
    		  $svyNGEvents.afterNGProcessedDOM(function () {
    			  // $evalAsync so that the dom is processed already by angular; for example if there's a "<label for=.." linked to this combobox,
    			  // the select's "id" attr must already be replaced by angular before we call select2
	    		  $(element).children("select").select2({
	    			  minimumResultsForSearch: -1, // don't show the search input when there are few items in combobox to choose from
	    			  containerCss: scope.style,
	    			  containerCssClass: select2Css
	    		  });
	    		  
		    	  element.on('$destroy', function() {
		    		  $(element).children("select").select2("destroy");
		    	  });
    		  }, true);
    	  } else scope.customClasses = (scope.model.styleClass ? scope.model.styleClass : "form-control input-sm svy-padding-xs");
      },
      templateUrl: 'servoydefault/combobox/combobox.html',
      replace: true
    };
}]);
