angular.module('svyCombobox',['servoy']).directive('svyCombobox', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel"
      },
      controller: function($scope, $element, $attrs) {
    	   $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
      },
      templateUrl: 'servoydefault/combobox/combobox.html',
      replace: true
    };
  })

  
  
  
  
