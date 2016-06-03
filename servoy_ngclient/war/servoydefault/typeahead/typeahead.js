angular.module('servoydefaultTypeahead', ['servoy'])
.directive('servoydefaultTypeahead', ['formatFilterFilter', '$apifunctions', '$svyProperties', '$formatterUtils', '$sabloConstants', function(formatFilter, $apifunctions, $svyProperties, $formatterUtils, $sabloConstants) {
	return {
		restrict: 'E',
		require: 'ngModel',
		scope: {
			model: "=svyModel",
			svyServoyapi: "=",
			handlers: "=svyHandlers",
			api: "=svyApi"
		},
		link: function($scope, $element, $attrs, ngModel) {

			$scope.onClick = function(event){
				if ($scope.model.editable == false && $scope.handlers.onActionMethodID)
				{
					$scope.handlers.onActionMethodID(event);
				}	
			}

			$scope.findMode = false;

			var hasRealValues = false;

			$scope.$watch('model.valuelistID', function() {
				if (!$scope.model.valuelistID || $scope.model.valuelistID.length == 0) return; // not loaded yet or already filtered
				hasRealValues = false;
				for (var i = 0; i < $scope.model.valuelistID.length; i++) {
					var item = $scope.model.valuelistID[i];
					if (item.realValue != item.displayValue) {
						hasRealValues = true;
						break;
					}
				}
			});

			$scope.$watch('model.dataProviderID', function() {
				$scope.refreshValue();
			});

			$scope.refreshValue= function (){
				if (!hasRealValues)
				{
					$scope.value = $scope.model.dataProviderID;
				}
				else
				{
					var found = false;
					if (angular.isDefined($scope.model.valuelistID)) {
        					for (var i = 0; i < $scope.model.valuelistID.length; i++) {
        						var item = $scope.model.valuelistID[i];
        						if (item.realValue === $scope.model.dataProviderID) {
        							$scope.value = item.displayValue;
        							found = true;
        							break;
        						}
        					}
					}
					if(!found)
					{
						$scope.value = null;
					}	
				}	 
			}

			var editing = false;

			$scope.startEdit = function() {
				editing = true;
			}

			$scope.doSvyApply = function() {
				if (!editing) 
					return;
				if ($('[typeahead-popup]').attr('aria-hidden') == "true") {
					editing = false;
					if ($scope.model.valuelistID) {
						var hasMatchingDisplayValue = false;
						for (var i = 0; i < $scope.model.valuelistID.length; i++) {
							if ($scope.value === $scope.model.valuelistID[i].displayValue) {
								hasMatchingDisplayValue = true;
								$scope.model.dataProviderID = $scope.model.valuelistID[i].realValue;
								break;
							}
						}
						if (!hasMatchingDisplayValue)
						{
							if (hasRealValues) 
							{
								$scope.model.dataProviderID = null;
							}
							else
							{
								$scope.model.dataProviderID = $scope.value;
							}	
						}	
					}
					else
					{
						$scope.model.dataProviderID = $scope.value;
					}	
					$scope.svyServoyapi.apply('dataProviderID');
				} 
				else if (!hasRealValues)
				{
					editing = false;
					$scope.model.dataProviderID = $scope.value;
					$scope.svyServoyapi.apply('dataProviderID');
				}
			}

			/**
			 * Request the focus to this typeahead.
			 * @example %%prefix%%%%elementName%%.requestFocus();
			 * @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
			 */
			$scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) { 
				if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID)
				{
					$element.unbind('focus');
					$element[0].focus();
					$element.bind('focus', $scope.handlers.onFocusGainedMethodID)
				}
				else
				{
					$element[0].focus();
				}
			}

			/**
			 * Returns the currently selected text in the specified typeahead. 
			 * @example var my_text = %%prefix%%%%elementName%%.getSelectedText();
			 * @return {String} The selected text in the text field.
			 */
			$scope.api.getSelectedText = $apifunctions.getSelectedText($element[0]);
			/**
			 * Replaces the selected text; if no text has been selected, the replaced value will be inserted at the last cursor position.
			 * @example %%prefix%%%%elementName%%.replaceSelectedText('John');
			 * @param s The replacement text.
			 */
			$scope.api.replaceSelectedText = $apifunctions.replaceSelectedText($element[0]);
			/**
			 * Selects all the contents of the typeahead.
			 * @example %%prefix%%%%elementName%%.selectAll();
			 */
			$scope.api.selectAll = $apifunctions.selectAll($element[0]);

			var storedTooltip = false;
			$scope.api.onDataChangeCallback = function(event, returnval) {
				var stringValue = typeof returnval == 'string'
					if (returnval === false || stringValue) {
						$element[0].focus();
						ngModel.$setValidity("", false);
						if (stringValue) {
							if (storedTooltip == false)
								storedTooltip = $scope.model.toolTipText;
							$scope.model.toolTipText = returnval;
						}
					} 
					else {
						ngModel.$setValidity("", true);
						if (storedTooltip !== false) $scope.model.toolTipText = storedTooltip;
						storedTooltip = false;
					}
			}

			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);
			
			var tooltipState = null;
			var formatState = null;
			var className = null;
			Object.defineProperty($scope.model,$sabloConstants.modelChangeNotifier, {configurable:true,value:function(property,value) {
				switch(property) {
					case "borderType":
						$svyProperties.setBorder($element,value);
						break;
					case "background":
					case "transparent":
						$svyProperties.setCssProperty($element,"backgroundColor",$scope.model.transparent?"transparent":$scope.model.background);
						break;
					case "foreground":
						$svyProperties.setCssProperty($element,"color",value);
						break;
					case "margin":
						if (value) $element.css(value);
						break;
					case "selectOnEnter":
						if (value) $svyProperties.addSelectOnEnter($element);
						break;
					case "styleClass":
						if (className) $element.removeClass(className);
						className = value;
						if(className) $element.addClass(className);
						break;
					case "fontType":
						$svyProperties.setCssProperty($element,"font",value);
						break;
					case "enabled":
						if (value) $element.removeAttr("disabled");
						else $element.attr("disabled","disabled");
						break;
					case "editable":
						if (value) $element.removeAttr("readonly");
						else $element.attr("readonly","readonly");
						break;
					case "toolTipText":
						if (tooltipState)
							tooltipState(value);
						else tooltipState = $svyProperties.createTooltipState($element,value);
					    break;
					case "format":
						if (formatState)
							formatState(value);
						else formatState = $formatterUtils.createFormatState($element, $scope, ngModel,true,value);
						break;
					case "horizontalAlignment":
						$svyProperties.setHorizontalAlignment($element,value);
						break;
					case "placeholderText":
						if (value)
							$element.attr("placeholder", value)
						else
							$element.removeAttr("placeholder");
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
			
		},
		templateUrl: 'servoydefault/typeahead/typeahead.html',
		replace: true
	};
}])
