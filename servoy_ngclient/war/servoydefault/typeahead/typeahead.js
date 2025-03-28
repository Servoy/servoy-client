angular.module('servoydefaultTypeahead', ['servoy'])
.directive('servoydefaultTypeahead', ['formatFilterFilter', '$apifunctions', '$svyProperties', '$formatterUtils', '$sabloConstants','$applicationService','$timeout', function(formatFilter, $apifunctions, $svyProperties, $formatterUtils, $sabloConstants,$applicationService, $timeout) {
	return {
		restrict: 'E',
		require: 'ngModel',
		scope: {
			model: "=svyModel",
			svyServoyapi: "=",
			handlers: "=svyHandlers",
			api: "=svyApi"
		},
		controller: function ($scope, $element) {
			$scope.showValues = null;
			if ($scope.model.clientProperty && $scope.model.clientProperty['TypeAhead'] && $scope.model.clientProperty['TypeAhead']['showPopupOnFocusGain'] !== null && $scope.model.clientProperty['TypeAhead']['showPopupOnFocusGain'] !== undefined )
			{
				$scope.showValues = $scope.model.clientProperty['TypeAhead']['showPopupOnFocusGain'];
			}
			if ($scope.showValues === null)
			{
				$scope.showValues = $applicationService.getUIProperty('TypeAhead.showPopupOnFocusGain');
			}	
			$scope.enterPressed = function(event)
			{
				if ($scope.handlers.onActionMethodID) {
					$scope.handlers.onActionMethodID(event)
				}
				$scope.value = $element.val();
                $scope.doSvyApply(false,true, event);
			};
		},
		link: function($scope, $element, $attrs, ngModel) {

			// add custom class to the popup, needed by ng-grids (ag-grid) so it can be used in form editors (popups)
			$timeout(function() {
				var ariaOwns = $element.attr("aria-owns");
				$("#" + ariaOwns).addClass("ag-custom-component-popup");
			}, 0, false);

			$scope.onFocus = function(){
				angular.element("[move-in-progress]").css("min-width",$element.outerWidth()+"px");
				if ($scope.showValues !== undefined && $scope.showValues != null)
				{
					return $scope.showValues;
				}
				return true;
			};
			var lastSetterFunction = null;
			function getSetterFunction() {
				if (lastSetterFunction) lastSetterFunction.enabled = false;
				function SetterFunction() {
					this.enabled = true;
					var myscope = this;
					this.setter = function(displayValue) {
						if (!myscope.enabled) return;
						$scope.value = displayValue;
					}
				}
				lastSetterFunction = new SetterFunction()
				return lastSetterFunction.setter;
			}
			
			$scope.onClick = function(event){
				if ($scope.model.editable == false && $scope.handlers.onActionMethodID)
				{
					$scope.handlers.onActionMethodID(event);
				}	
			}

			$scope.findMode = false;

			var hasRealValues = undefined;
			var editing = false;
			
			$scope.$watch('model.isOpened', function(){
				var bodyElements = document.querySelectorAll('.svy-body,.ui-grid-viewport');				
				for(var i = 0; i < bodyElements.length; i++){
					if($scope.model.isOpened)
			           bodyElements[i].addEventListener('scroll',$scope.fireRecalculating);
					else bodyElements[i].removeEventListener('scroll',$scope.fireRecalculating);
				}				
			})
			
			$scope.$watch('model.valuelistID', function() {
				if (!$scope.model.valuelistID) return; // not loaded yet or already filtered
				hasRealValues =  $scope.model.valuelistID.hasRealValues();
				if (hasRealValues && !editing)
				{
					$scope.refreshValue(false);
				}	
			});

			$scope.$watch('model.dataProviderID', function() {
				$scope.refreshValue(true);
			});

			$scope.refreshValue= function (retrieveFromServer){
				if(hasRealValues == undefined) {
					if (angular.isDefined($scope.model.valuelistID)) 
					{
						$scope.model.valuelistID.getDisplayValue($scope.model.dataProviderID).then(getSetterFunction());
					}
				}
				else if (!hasRealValues)
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
        						} else if (item.realValue && $scope.model.dataProviderID instanceof Date && $scope.model.dataProviderID.getTime() == moment(item.realValue).toDate().getTime()){
        						    $scope.value = item.displayValue;
                                    found = true;
                                    break;
        						}
        					}
					}
					if(retrieveFromServer && !found)
					{
						$scope.value = null;
						if (angular.isDefined($scope.model.valuelistID)) 
						{
							$scope.model.valuelistID.getDisplayValue($scope.model.dataProviderID).then(getSetterFunction());
						}
					}	
				}	 
			}

			$scope.startEdit = function() {
				editing = true;
				$scope.svyServoyapi.startEdit("dataProviderID")
				if (setCaret)
				{
					setCaret = false;
					var len = $element[0].value.length;
					$element[0].setSelectionRange(len,len);
				}
			}

			$scope.doSvyApply = function(isSelectFromPopup, doApply, event) {
				if (!editing || ($element.attr("readonly") == "readonly")) 
					return;
				if (isSelectFromPopup || !$scope.model.isOpened) {
					if(!isSelectFromPopup) {
						editing = false;
					}
					if ($scope.model.valuelistID) {
						var hasMatchingDisplayValue = false;
						for (var i = 0; i < $scope.model.valuelistID.length; i++) {
							// compare trimmed values, typeahead will trim the selected value
							if ($.trim($scope.value) === $.trim($scope.model.valuelistID[i].displayValue)) {
								hasMatchingDisplayValue = true;
								if ($scope.model.dataProviderID === $scope.model.valuelistID[i].realValue)
								{
									// same value, do not send again to server
									return;
								}	
								$scope.model.dataProviderID = $scope.model.valuelistID[i].realValue;
								break;
							}
						}
						if (!hasMatchingDisplayValue)
						{
							if (hasRealValues && !$scope.model.findmode) 
							{
								// if we still have old value do not set it to null or try to  get it from the list.
								if ($scope.model.dataProviderID != null && $scope.model.dataProviderID !== $scope.value)
								{
									// so invalid thing is typed in the list and we are in real/display values, try to search the real value again to set the display value back.
									$scope.model.valuelistID.getDisplayValue($scope.model.dataProviderID).then(getSetterFunction());
								}	
								// if the dataproviderid was null and we are in real|display then reset the value to ""
								else if($scope.model.dataProviderID == null) {
									if ($scope.value !== "")
									{
										$scope.value = "";
									}
									else
									{
										// do not push, is not changed, maybe null is not allowed
										return;
									}	
								}
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
				else if (!hasRealValues && ($scope.model.dataProviderID != $scope.value))
				{
                    var apply = true;
                    if (event && event.originalEvent && event.originalEvent.relatedTarget) {
                        apply = !event.originalEvent.relatedTarget.parentElement.classList.contains("uib-typeahead-match");
                    }
                    if (apply) {
                        editing = false;
                        $scope.model.dataProviderID = $scope.value;
                        $scope.svyServoyapi.apply('dataProviderID');
                    } else {
                        $timeout(function(triggerValue){
                            if (triggerValue == $scope.value) {
                                $scope.doSvyApply(false,true);
                            }
                        },100,true,$scope.value)
                    }
				}
				else if (hasRealValues) {
					$timeout(function(triggerValue){
						if (triggerValue == $scope.value) {
							$scope.doSvyApply(true);
						}
					},100,true,$scope.value)
				}
			}

			var setCaret = false;
			$scope.doSelect = function($item, $model, $label, $event) {
				$scope.startEdit();
				$scope.doSvyApply(true);
				setCaret = true;//when the focus is back to the field, set the cursor on the last position
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
			$svyProperties.createTooltipState($element, function() { return $scope.model.toolTipText });
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