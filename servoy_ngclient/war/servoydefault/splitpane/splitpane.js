angular.module('svySplitpane',['servoy']).directive('svySplitpane', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        svyServoyapi: "=",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	  $scope.$watch("model.readOnly", function(newValue) {
    		  if ($scope.model.tabs[0] && $scope.model.tabs[0].containsFormId)
    		  {
    			  $scope.svyServoyapi.setFormReadOnly($scope.model.tabs[0].containsFormId,newValue);
    		  }
    		  if ($scope.model.tabs[1] && $scope.model.tabs[1].containsFormId)
    		  {
    			  $scope.svyServoyapi.setFormReadOnly($scope.model.tabs[1].containsFormId,newValue);
    		  }
    	  });
    	  $scope.$watch("model.enabled", function(newValue) {
    		  if ($scope.model.tabs[0] && $scope.model.tabs[0].containsFormId)
    		  {
    			  $scope.svyServoyapi.setFormEnabled($scope.model.tabs[0].containsFormId,newValue);
    		  }
    		  if ($scope.model.tabs[1] && $scope.model.tabs[1].containsFormId)
    		  {
    			  $scope.svyServoyapi.setFormEnabled($scope.model.tabs[1].containsFormId,newValue);
    		  }
    	  });
    	  $scope.svyServoyapi.setFormVisibility($scope.model.tabs[0].containsFormId, true, $scope.model.tabs[0].relationName,0);
    	  $scope.svyServoyapi.setFormVisibility($scope.model.tabs[1].containsFormId, true, $scope.model.tabs[1].relationName,1);
    	  $scope.onChange = function() {
    		  if($scope.handlers.onChangeMethodID) $scope.handlers.onChangeMethodID(-1,event);
    	  }
    	  
          $scope.getForm = function(tab) {
        	  return $scope.svyServoyapi.getFormUrl(tab.containsFormId);
          }
      },
      templateUrl: 'servoydefault/splitpane/splitpane.html',
      replace: true
    };
  })