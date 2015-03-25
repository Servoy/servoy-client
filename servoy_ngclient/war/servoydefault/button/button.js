angular.module('servoydefaultButton',['servoy']).directive('servoydefaultButton', function(formatFilterFilter) {  
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
      },
      templateUrl: 'servoydefault/button/button.html'
    };
  })
  
  
  
