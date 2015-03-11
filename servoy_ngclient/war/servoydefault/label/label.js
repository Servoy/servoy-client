angular.module('servoydefaultLabel',['servoy'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("servoydefault/label/label.html").then(function(result){
		$templateCache.put("template/servoydefault/label/label.html", result.data);
    });
	$http.get("servoydefault/label/labelfor.html").then(function(result){
		$templateCache.put("template/servoydefault/label/labelfor.html", result.data);
    });	
}])
.directive('servoydefaultLabel', ['$parse','$templateCache','$compile',function($parse,$templateCache,$compile) {
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      	handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
    	  $element.html($templateCache.get($scope.model.labelFor && ($attrs.headercell == undefined) ? "template/servoydefault/label/labelfor.html" : "template/servoydefault/label/label.html"));
          $compile($element.contents())($scope);
    	  
          $scope.containerstyle = {overflow:'hidden',position:'absolute'}
          $scope.contentstyle = {width:'100%',overflow:'hidden',position:'absolute',whiteSpace:'nowrap'}
          $scope.getClass = function() {
        	  var classes = "";
        	  if($scope.model.styleClass) {
        		  classes += $scope.model.styleClass;
        	  }
        	  else {
        		  classes += "svy-label";
        	  }
        	  if($scope.model.showFocus && ($scope.handlers.onActionMethodID || $scope.handlers.onDoubleClickMethodID || $scope.handlers.onRightClickMethodID)) {
        		  classes += " svy-label-with-focus"
        	  }

        	  return classes;
          }
      }
    };
}])