angular.module('svyTypeahead',['servoy']).directive('svyTypeahead', function($timeout) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        svyApply: "=",
        handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
    	  $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	  
          var timeoutPromise = null;
          var lastAppliedDataProviderID = null;

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
      },
      templateUrl: 'servoydefault/typeahead/typeahead.html',
      replace: true
    };
  })

  
  
  
  
