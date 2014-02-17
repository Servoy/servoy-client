servoyModule.directive('svyTypeahead', function($servoy,$timeout,$utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        svyApply: "="
      },
      link: function($scope, $element, $attrs) {
    	  $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
          $utils.watchProperty($scope,'model.background',$scope.style,'backgroundColor')
          $utils.watchProperty($scope,'model.foreground',$scope.style,'color')
    	  
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
      },
      templateUrl: 'servoydefault/typeahead/typeahead.html',
      replace: true
    };
  })

  
  
  
  
