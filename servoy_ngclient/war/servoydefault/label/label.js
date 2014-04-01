angular.module('svyLabel',['servoy'])
.run(["$templateCache","$http",function($templateCache,$http){
	$http.get("servoydefault/label/label.html").then(function(result){
		$templateCache.put("template/servoydefault/label/label.html", result.data);
    });
	$http.get("servoydefault/label/labelfor.html").then(function(result){
		$templateCache.put("template/servoydefault/label/labelfor.html", result.data);
    });	
}])
.directive('svyLabel', ["formatFilterFilter",'$utils','$parse','$templateCache','$compile',function(formatFilter,$utils,$parse,$templateCache,$compile) {
    return {
      restrict: 'E',
      transclude: true,
      scope: {
      	model: "=svyModel"
      },
      link: function($scope, $element, $attrs) {
    	  
    	  $element.html($templateCache.get($scope.model.labelFor && ($attrs.headercell == undefined) ? "template/servoydefault/label/labelfor.html" : "template/servoydefault/label/label.html"));
          $compile($element.contents())($scope);
    	  
          $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
          $scope.bgstyle = {};
          
          if ($scope.model.imageMediaID) {
    		  $scope.bgstyle = {width:'100%',height:'100%',overflow:'hidden'};
    		  $scope.bgstyle['background-image'] = "url('" + $scope.model.imageMediaID + "')"; 
    		  $scope.bgstyle['background-repeat'] = "no-repeat";
    		  $scope.bgstyle['background-position'] = "left";
    		  $scope.bgstyle['background-size'] = "contain";
    		  $scope.bgstyle['display'] = "inline-block";
    		  $scope.bgstyle['vertical-align'] = "middle"; 
    	  }
          
          if ($scope.model.textRotation && $scope.model.textRotation != 0)
          {
        	  var rotation = $scope.model.textRotation;
        	  var r = 'rotate(' + rotation + 'deg)';
        	  $scope.bgstyle['-moz-transform'] = r;
        	  $scope.bgstyle['-webkit-transform'] = r;
        	  $scope.bgstyle['-o-transform'] = r;
        	  $scope.bgstyle['-ms-transform'] = r;
        	  $scope.bgstyle['transform'] = r;
        	  $scope.bgstyle['position'] = 'absolute';
        	  
        	  if (rotation == 90 || rotation == 270)
        	  {
        		  $scope.bgstyle['width'] = $scope.model.size.height+'px';
        		  $scope.bgstyle['height'] = $scope.model.size.width+'px';
        		  $scope.bgstyle['left'] =  ($scope.model.size.width -$scope.model.size.height)/2 +'px';
        		  $scope.bgstyle['top'] = ($scope.model.size.height -$scope.model.size.width)/2 +'px';
        	  }
          }
      },
      replace: true
    };
}])