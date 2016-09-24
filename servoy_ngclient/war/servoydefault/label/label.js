angular.module('servoydefaultLabel',['servoy'])
.directive('servoydefaultLabel', ['$http','$templateCache','$compile','$apifunctions','$sabloConstants','$svyProperties',function($http,$templateCache,$compile,$apifunctions,$sabloConstants,$svyProperties) {
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
      	handlers: "=svyHandlers",
        api: "=svyApi",
		servoyApi: "=svyServoyapi"
      },
      link: function($scope, $element, $attrs) {
		var templateUrl = $scope.model.labelFor && ($attrs.headercell == undefined) ? "servoydefault/label/labelfor.html" : "servoydefault/label/label.html";
		$http.get(templateUrl, {cache: $templateCache}).then(function(result) {
    	  	$element.html(result.data);
          	$compile($element.contents())($scope);
    	  	var tooltipState = null;
			var className = null;
			var element = $element.children().first();
			Object.defineProperty($scope.model,$sabloConstants.modelChangeNotifier, {configurable:true,value:function(property,value) {
				switch(property) {
					case "borderType":
						$svyProperties.setBorder(element,value);
						break;
					case "background":
					case "transparent":
						$svyProperties.setCssProperty(element,"backgroundColor",$scope.model.transparent?"transparent":$scope.model.background);
						break;
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
					case "foreground":
						$svyProperties.setCssProperty(element,"color",value);
						break;
					case "fontType":
						$svyProperties.setCssProperty(element,"font",value);
						break;
					case "horizontalAlignment":
						$svyProperties.setHorizontalAlignment(element.children().first(),value);
						break;
					case "margin":
						if (value) element.css(value);
						break;
					case "mnemonic":
						if (value) element.attr('accesskey',value);
						else element.removeAttr('accesskey');
						break;
					case "rolloverCursor":
						element.css('cursor',value == 12?'pointer':'default');
						break
					case "styleClass":
						if (className) element.removeClass(className);
						className = value;
						if(className) element.addClass(className);
						break;
					case "showFocus":
						if ($scope.model.showFocus && ($scope.handlers.onActionMethodID || $scope.handlers.onDoubleClickMethodID || $scope.handlers.onRightClickMethodID)) 
							element.addClass("svy-label-with-focus");
						else element.removeClass("svy-label-with-focus");
						break;
					case "textRotation":
						$svyProperties.setRotation(element,$scope,value);
						break
					case "toolTipText":
						if (tooltipState)
							tooltipState(value);
						else tooltipState = $svyProperties.createTooltipState(element,value);
					    break;
					case "verticalAlignment":
						$svyProperties.setVerticalAlignment(element.children().first(),value);
						break;
				}
			}});
			var destroyListenerUnreg = $scope.$on("$destroy", function() {
				destroyListenerUnreg();
				delete $scope.model[$sabloConstants.modelChangeNotifier];
			});
			// data can already be here, if so call the modelChange function so that it is initialized correctly.
			var modelChangFunction = $scope.model[$sabloConstants.modelChangeNotifier];
			for (var key in $scope.model) {
				modelChangFunction(key,$scope.model[key]);
			}
          
			if ($scope.model.dataProviderID === undefined && $scope.model.text === undefined && $scope.model.imageMediaID)
			{
				//image only, set line-height to default
				$svyProperties.setCssProperty(element,"line-height","100%");
			}	  
			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);
		  
			$scope.hasText = function() {
				var txtValue = $scope.model.dataProviderID == null || $scope.model.dataProviderID.url ? $scope.model.text : $scope.model.dataProviderID;
				return (txtValue && txtValue.length > 0);
			}
    	});
      }
    };
}])