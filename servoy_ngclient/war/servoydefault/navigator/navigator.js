servoyModule.directive('svyNavigator', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {  
    	  
    	  $scope.editIndex = 0;
    	  $scope.$watch('model.currentIndex', function (newVal, oldVal, scope) {
    		  if(!newVal) return;
    		  $scope.editIndex = newVal
    	  })
    	  
    	  $scope.setIndex =  function (idx){
    		  var i = parseInt(idx)
    		  $scope.handlers.setSelectedIndex(i);	
    	  }
    	  
      },
      templateUrl: 'servoydefault/navigator/navigator.html',
      replace: true
      
    };
})