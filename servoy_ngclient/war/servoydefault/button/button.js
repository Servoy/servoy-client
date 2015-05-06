angular.module('servoydefaultButton',['servoy']).directive('servoydefaultButton', function(formatFilterFilter, $apifunctions) {  
    return {
      restrict: 'E',
      scope: {
       	model: "=svyModel",
       	handlers: "=svyHandlers",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	  $scope.containerstyle = {overflow:'hidden',position:'absolute'}
          $scope.contentstyle = {width:'100%',overflow:'hidden',position:'relative',whiteSpace:'nowrap'}

    	  /**
    	   * Set the focus to this button.
    	   * @example %%prefix%%%%elementName%%.requestFocus();
    	   */
    	  $scope.api.requestFocus = function() { 
    		  $element.find('button')[0].focus();
    	  }
    	  
    	  $scope.api.getWidth = $apifunctions.getWidth($element[0]);
    	  $scope.api.getHeight = $apifunctions.getHeight($element[0]);
    	  $scope.api.getLocationX = $apifunctions.getX($element[0]);
    	  $scope.api.getLocationY = $apifunctions.getY($element[0]);    	  
      },
      templateUrl: 'servoydefault/button/button.html'
    };
  })
  
  
  
