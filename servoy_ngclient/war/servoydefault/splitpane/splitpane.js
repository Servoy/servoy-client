servoyModule.directive('svySplitpane', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        svyServoyapi: "="
      },
      controller: function($scope, $element, $attrs) {
    	  $scope.svyServoyapi.setFormVisibility($scope.model.tabs[0].containsFormId, true, $scope.model.tabs[0].relationName);
    	  $scope.svyServoyapi.setFormVisibility($scope.model.tabs[1].containsFormId, true, $scope.model.tabs[1].relationName);
      },
      templateUrl: 'servoydefault/splitpane/splitpane.html',
      replace: true
    };
  })