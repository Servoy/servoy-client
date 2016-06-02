angular.module('bootstrapcomponentsCheckbox',['servoy']).directive('bootstrapcomponentsCheckbox', function() {  
    return {
      restrict: 'E',
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi",
        svyServoyapi: "="
      },
      link: function($scope, $element, $attrs) {

          $scope.selection = false;
          
          $scope.$watch('model.dataProviderID', function() 
          { 
        	  $scope.selection = getSelectionFromDataprovider();
          })
          
          $scope.checkBoxClicked = function()
          {
        	  if ($scope.model.valuelistID && $scope.model.valuelistID[0]) 
        	  {
					$scope.model.dataProviderID = $scope.model.dataProviderID == $scope.model.valuelistID[0].realValue ? null : $scope.model.valuelistID[0].realValue;
			  } 
        	  else if (angular.isString($scope.model.dataProviderID))
        	  {
        		  $scope.model.dataProviderID = $scope.model.dataProviderID == "1" ? "0" : "1";
        	  }
        	  else
        	  {
        		  $scope.model.dataProviderID = $scope.model.dataProviderID > 0 ?  0 :  1;
        	  }
        	  $scope.svyServoyapi.apply('dataProviderID')
          }
          
          function getSelectionFromDataprovider()
          {
              if(!$scope.model.dataProviderID) return false;
              if ($scope.model.valuelistID && $scope.model.valuelistID[0]) 
              {
					return $scope.model.dataProviderID == $scope.model.valuelistID[0].realValue;
              }
              if (angular.isString($scope.model.dataProviderID))
        	  {
        		 return $scope.model.dataProviderID == "1";
        	  }
        	  else
        	  {
        		  return $scope.model.dataProviderID > 0;
        	  }
          }
      },
      templateUrl: 'bootstrapcomponents/checkbox/checkbox.html'
    };
})