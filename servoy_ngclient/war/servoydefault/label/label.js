angular.module('servoydefaultLabel',['servoy'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("servoydefault/label/label.html").then(function(result){
		$templateCache.put("template/servoydefault/label/label.html", result.data);
    });
	$http.get("servoydefault/label/labelfor.html").then(function(result){
		$templateCache.put("template/servoydefault/label/labelfor.html", result.data);
    });	
}])
.directive('servoydefaultLabel', ["formatFilterFilter",'$utils','$parse','$templateCache','$compile',function(formatFilter,$utils,$parse,$templateCache,$compile) {
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      	handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
    	  
    	  $element.html($templateCache.get($scope.model.labelFor && ($attrs.headercell == undefined) ? "template/servoydefault/label/labelfor.html" : "template/servoydefault/label/label.html"));
          $compile($element.contents())($scope);
    	  
          $scope.containerstyle = {overflow:'hidden'}
          $scope.contentstyle = {width:'100%',overflow:'hidden',whiteSpace:'nowrap'}
          
      }
    };
}])