angular.module('svyTypeahead',['servoy'])
.directive('svyTypeahead',['$timeout','formatFilterFilter', function($timeout,formatFilter) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        svyApply: "=",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {
    	  $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	  $scope.findMode = false;
          var timeoutPromise = null;
          var lastAppliedDataProviderID = null;
          
         $scope.formatLabel = function (model){
        	  var displayFormat = undefined;
     		  var type = undefined;
     		  if($scope.model.format && $scope.model.format.display) displayFormat = $scope.model.format.display;
     		  if($scope.model.format && $scope.model.format.type) type = $scope.model.format.type;	          		
     		  return formatFilter(model, displayFormat ,type);        	 
         }
          $scope.doSvyApply = function (){
            // only the last ngBlur should take effect
           if(timeoutPromise) $timeout.cancel(timeoutPromise); 
              
           timeoutPromise = $timeout(function(){
                 // can be onblur because an item from the dropdown was clicked and right after the user goes elsewhere and another onblur is triggered
               if($scope.model.dataProviderID !=lastAppliedDataProviderID){
                $scope.svyApply('dataProviderID')
               }
               lastAppliedDataProviderID = $scope.model.dataProviderID
            },100);
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
      templateUrl: 'servoydefault/typeahead/typeahead.html',
      replace: true
    };
  }])

  
  
  
  
