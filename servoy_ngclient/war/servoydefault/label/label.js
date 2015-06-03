angular.module('servoydefaultLabel',['servoy'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("servoydefault/label/label.html").then(function(result){
		$templateCache.put("template/servoydefault/label/label.html", result.data);
    });
	$http.get("servoydefault/label/labelfor.html").then(function(result){
		$templateCache.put("template/servoydefault/label/labelfor.html", result.data);
    });	
}])
.directive('servoydefaultLabel', ['$parse','$templateCache','$compile','$apifunctions',function($parse,$templateCache,$compile,$apifunctions) {
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      	handlers: "=svyHandlers",
        api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {
    	  $element.html($templateCache.get($scope.model.labelFor && ($attrs.headercell == undefined) ? "template/servoydefault/label/labelfor.html" : "template/servoydefault/label/label.html"));
          $compile($element.contents())($scope);
    	  
          $scope.containerstyle = {overflow:'hidden',position:'absolute'}
          $scope.contentstyle = {width:'100%',overflow:'hidden',position:'absolute',whiteSpace:'nowrap'}
          $scope.$watch("model.enabled", function(newValue,oldValue) {
        	  if (!newValue) {
        		  $scope.containerstyle.filter =  "alpha(opacity=50)"
        		  $scope.containerstyle['-moz-opacity'] = ".50";
        		  $scope.containerstyle.opacity = ".50";
        	  }
        	  else {
        		  delete $scope.containerstyle.filter;
        		  delete $scope.containerstyle['-moz-opacity'];
        		  delete $scope.containerstyle.opacity;
        	  }
          });
          $scope.getClass = function() {
        	  var classes = "svy-label";
        	  if($scope.model.styleClass) {
        		  classes += " " + $scope.model.styleClass;
        	  }
        	  if($scope.model.showFocus && ($scope.handlers.onActionMethodID || $scope.handlers.onDoubleClickMethodID || $scope.handlers.onRightClickMethodID)) {
        		  classes += " svy-label-with-focus"
        	  }

        	  return classes;
          }
          
    	  $scope.api.getWidth = $apifunctions.getWidth($element[0]);
    	  $scope.api.getHeight = $apifunctions.getHeight($element[0]);
    	  $scope.api.getLocationX = $apifunctions.getX($element[0]);
    	  $scope.api.getLocationY = $apifunctions.getY($element[0]);          
      }
    };
}])