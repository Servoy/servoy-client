angular.module('servoydefaultRadio',['servoy']).directive('servoydefaultRadio', function($apifunctions) {  
    return {
      restrict: 'E',
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi",
        svyServoyapi: "="
      },
      link: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%'}  
          
          $scope.radioClicked = function()
          {
        	  $scope.model.dataProviderID = $scope.model.valuelistID[0].realValue;
        	  $scope.svyServoyapi.apply('dataProviderID')
          }
          
          /**
           * Request the focus to this radio button.
           * @example %%prefix%%%%elementName%%.requestFocus();
           * @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
           */
          $scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) { 
        	  var input = $element.find('input');
        	  if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID)
        	  {
        		  input.unbind('focus');
        		  input[0].focus();
        		  input.bind('focus', $scope.handlers.onFocusGainedMethodID)
        	  }
        	  else
        	  {
        		  input[0].focus();
        	  }
          }
          
    	  $scope.api.getWidth = $apifunctions.getWidth($element[0]);
    	  $scope.api.getHeight = $apifunctions.getHeight($element[0]);
    	  $scope.api.getLocationX = $apifunctions.getX($element[0]);
    	  $scope.api.getLocationY = $apifunctions.getY($element[0]);
      },
      templateUrl: 'servoydefault/radio/radio.html'
    };
})