angular.module('bootstrapcomponentsLabel',['servoy'])
.directive('bootstrapcomponentsLabel',['$templateCache','$compile','$http', function($templateCache,$compile,$http) {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	var templateUrl = $scope.model.labelFor ? "bootstrapcomponents/label/labelfor.html" : "bootstrapcomponents/label/label.html";
  		$http.get(templateUrl, {cache: $templateCache}).then(function(result) {
      	  $element.html(result.data);
          $compile($element.contents())($scope);
  		});
      },
      templateUrl: 'bootstrapcomponents/label/label.html'
    };
  }])
  
  
  
