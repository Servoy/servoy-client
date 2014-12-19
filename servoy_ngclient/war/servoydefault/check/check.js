angular.module('servoydefaultCheck',['servoy']).directive('servoydefaultCheck', function() {  
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
          $scope.style = {width:'100%',height:'100%'}

          $scope.selection = false;
          
          $scope.$watch('model.dataProviderID', function() 
          { 
        	  $scope.selection = getSelectionFromDataprovider();
          })
          
          $scope.checkBoxClicked = function()
          {
        	  if ($scope.model.valuelistID && $scope.model.valuelistID[0])
        	  {
        		  $scope.model.dataProviderID = $scope.model.dataProviderID == $scope.model.valuelistID[0].realValue ?  null : $scope.model.valuelistID[0].realValue;
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
          
          $scope.api.setValueListItems = function(values) 
          {
        	  var valuelistItems = [];
        	  for (var i = 0; i < values.length; i++)
        	  {
        		  var item = {};
        		  item['displayValue'] = values[i][0];
        		  if (values[i][1] !== undefined)
        		  {
        			  item['realValue'] = values[i][1];
        		  }
        		  valuelistItems.push(item); 
        	  }
        	  $scope.model.valuelistID = valuelistItems;
          }
          
          function getSelectionFromDataprovider()
          {
              if(!$scope.model.dataProviderID) return false;
              if ($scope.model.valuelistID && $scope.model.valuelistID[0])
        	  {
        		  return $scope.model.dataProviderID == $scope.model.valuelistID[0].realValue;
        	  }  
        	  else if (angular.isString($scope.model.dataProviderID))
        	  {
        		 return $scope.model.dataProviderID == "1";
        	  }
        	  else
        	  {
        		  return $scope.model.dataProviderID > 0;
        	  }
          }
          
         // special method that servoy calls when this component goes into find mode.
      	 $scope.api.setFindMode = function(findMode, editable) {
      	 	if (findMode)
      	 	{
      	 		$scope.wasEditable = $scope.model.editable;
      	 		if (!$scope.model.editable) $scope.model.editable = editable;
      	 	}
      	 	else
      	 	{
      	 		$scope.model.editable = $scope.wasEditable != undefined ? $scope.wasEditable : editable;
      	 	}
      	 };              
      },
      templateUrl: 'servoydefault/check/check.html'
    };
})