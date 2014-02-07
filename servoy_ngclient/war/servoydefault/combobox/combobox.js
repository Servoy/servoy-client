servoyModule.directive('svyCombobox', function($servoy) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel"
      },
      controller: function($scope, $element, $attrs) {
      },
      templateUrl: 'servoydefault/combobox/combobox.html',
      replace: true
    };
  })

  
  
  
  
