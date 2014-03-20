servoyModule.directive('svyImagemedia', function($utils, $window, $document) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        handlers: "=svyHandlers",
      },
      controller: function($scope, $element, $attrs) {  

    	  $scope.imageURL = '';
    	  
          $scope.$watch('model.dataProviderID', function(){
        	  $scope.imageURL = ($scope.model.dataProviderID && $scope.model.dataProviderID.url) ? ($scope.model.dataProviderID.contentType && ($scope.model.dataProviderID.contentType.indexOf("image") == 0) ? $scope.model.dataProviderID.url : "servoydefault/imagemedia/res/images/notemptymedia.gif") : "servoydefault/imagemedia/res/images/empty.gif";
           })
    	  
    	  $scope.download = function() {
        	  if($scope.model.dataProviderID && $scope.model.dataProviderID.url) {
	    		  var x = 0, y = 0;
	    		  if ($document.all) {
	    			  x = $window.screenTop + 100;
	    			  y = $window.screenLeft + 100;
	    		  } else if ($document.layers) {
	    			  x = $window.screenX + 100;
	    			  y = $window.screenY + 100;
	    		  } else { // firefox, need to switch the x and y?
	    			  y = $window.screenX + 100;
	    			  x = $window.screenY + 100;
	    		  }
	    		  $window.open($scope.model.dataProviderID.url,'download','top=' + x + ',left=' + y + ',screenX=' + x + ',screenY=' + y + ',location=no,toolbar=no,menubar=no,width=310,height=140,resizable=yes');
        	  }
    	  }
    	  
    	  $scope.clear = function() {
    		  $scope.model.dataProviderID = null;
    		  $scope.handlers.svy_apply('dataProviderID');
    	  }
      },
      templateUrl: 'servoydefault/imagemedia/imagemedia.html',
      replace: true
    };
  })