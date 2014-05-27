angular.module('svyRadio',['servoy']).directive('svyRadio', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%'}  
          
          $scope.radioClicked = function()
          {
        	  $scope.model.dataProviderID = $scope.model.valuelistID[0].realValue;
        	  $scope.handlers.svy_apply('dataProviderID')
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
      },
      templateUrl: 'servoydefault/radio/radio.html',
      replace: true
    };
})