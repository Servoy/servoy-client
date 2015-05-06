angular.module('servoydefaultRectangle',['servoy'])
.directive('servoydefaultRectangle', function($apifunctions) {
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      	handlers: "=svyHandlers",
        api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {
          $scope.$watch('model.borderType', function(){
              setupRectangle();
          })
          $scope.$watch('model.foreground', function(){
        	  if($scope.model.borderType && $scope.model.borderType.borderStyle) {
        		  $scope.model.borderType.borderStyle.borderColor = $scope.model.foreground ? $scope.model.foreground : "#000000";
        	  }
          }, true)
          $scope.$watch('model.size', function(){
        	  // shape type 3 == oval
        	  if($scope.model.shapeType == 3 && $scope.model.borderType && $scope.model.borderType.borderStyle) {
        		  $scope.model.borderType.borderStyle.borderRadius = $scope.model.size.width / 2 + "px";
        	  }  
          })
          
          function setupRectangle() {
        	  if(!$scope.model.borderType) {
        		  $scope.model.borderType = new Object();
        		  $scope.model.borderType.borderStyle = {borderStyle: "solid"};
        	  }
        	  if($scope.model.borderType.borderStyle) {
        		  $scope.model.borderType.borderStyle.borderWidth = $scope.model.lineSize + "px";
        		  $scope.model.borderType.borderStyle.borderColor = $scope.model.foreground ? $scope.model.foreground : "#000000";
        	  }
        	  
        	  if($scope.model.roundedRadius) {
        		  $scope.model.borderType.borderStyle.borderRadius = $scope.model.roundedRadius / 2 + "px"; 
        	  } else if ($scope.model.shapeType == 3) {
        		  $scope.model.borderType.borderStyle.borderRadius = $scope.model.size.width / 2 + "px";
        	  }  
          }
          
    	  $scope.api.getWidth = $apifunctions.getWidth($element[0]);
    	  $scope.api.getHeight = $apifunctions.getHeight($element[0]);
    	  $scope.api.getLocationX = $apifunctions.getX($element[0]);
    	  $scope.api.getLocationY = $apifunctions.getY($element[0]);
      },
      templateUrl: 'servoydefault/rectangle/rectangle.html'
    };
})