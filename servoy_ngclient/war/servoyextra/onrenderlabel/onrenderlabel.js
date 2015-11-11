angular.module('servoyextraOnrenderlabel',['servoy'])
.directive('servoyextraOnrenderlabel', ['$parse','$templateCache','$apifunctions','$sabloConstants','$svyProperties',function($parse,$templateCache,$apifunctions,$sabloConstants,$svyProperties) {
    return {
      restrict: 'E',
      templateUrl: 'servoyextra/onrenderlabel/onrenderlabel.html',
      scope: {
      	model: "=svyModel",
      	handlers: "=svyHandlers",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	  if ( $scope.handlers.onActionMethodID) {
    		  // in readonly tableview we need cell navigation/cell focus
    		  // this disabled the click on the outer div so we need he inner div also to act as the click
    		  // but this triggers outside of the tableview duplicate clicks so prevent the same event (with the same timestamp) to be executed twice.
	    	  var lastEvent = null;
	    	  $scope.onclick = function(event) {
	    		  if (lastEvent) {
	    			  if (lastEvent.timeStamp == event.timeStamp){
	    				  return;
	    			  }
	    		  }
	    		  lastEvent = event;
	        	  $scope.handlers.onActionMethodID(event);
	          }
    	  }
      },
      link: function($scope, $element, $attrs) {
    	  
			var tooltipState = null;
			var className = null;
			var classExpresion = null;
			var element = $element.children().first();
			Object.defineProperty($scope.model,$sabloConstants.modelChangeNotifier, {configurable:true,value:function(property,value) {
				switch(property) {
					case "enabled":
						if(value) {
							var css = {filter:"",opacity:"",pointerEvents:""}
							css['-moz-opacity'] = "";
							element.css(css);
						} else {
							var css = {filter:"alpha(opacity=50)",opacity:".50",pointerEvents:"none"}
							css['-moz-opacity'] = ".50";
							element.css(css);
						}
						break;
					case "styleClass":
						if (className) element.removeClass(className);
						className = value;
						if(className) element.addClass(className);
						break;
					case "styleClassExpression":
						if (classExpresion) element.removeClass(classExpresion);
						classExpresion = value;
						if(classExpresion) element.addClass(classExpresion);
						break;
					case "toolTipText":
						if (tooltipState)
							tooltipState(value);
						else tooltipState = $svyProperties.createTooltipState(element,value);
					    break;
				}
			}});
			var destroyListenerUnreg = $scope.$on("$destroy", function() {
				destroyListenerUnreg();
				delete $scope.model[$sabloConstants.modelChangeNotifier];
			});
			// data can already be here, if so call the modelChange function so that it is initialized correctly.
			var modelChangFunction = $scope.model[$sabloConstants.modelChangeNotifier];
			for (key in $scope.model) {
				modelChangFunction(key,$scope.model[key]);
			}
      }
    };
}])