angular.module('bootstrapcomponentsLabel',['servoy'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("bootstrapcomponents/label/label.html").then(function(result){
		$templateCache.put("template/bootstrapcomponents/label/label.html", result.data);
    });
	$http.get("bootstrapcomponents/label/labelfor.html").then(function(result){
		$templateCache.put("template/bootstrapcomponents/label/labelfor.html", result.data);
    });	
}])
.directive('bootstrapcomponentsLabel',['$templateCache','$compile', function($templateCache,$compile) {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
    	  
    	  $element.html($templateCache.get($scope.model.labelFor ? "template/bootstrapcomponents/label/labelfor.html" : "template/bootstrapcomponents/label/label.html"));
          $compile($element.contents())($scope);
      },
      templateUrl: 'bootstrapcomponents/label/label.html'
    };
  }])
  
  
  
