angular.module('servoydefaultCalendar', [ 'servoy' ]).directive('servoydefaultCalendar', function($log, $apifunctions, 
		$svyProperties, $sabloConstants, $sabloApplication, $applicationService, $animate, $utils) {
	return {
		restrict : 'E',
		scope : {
			model : "=svyModel",
			handlers : "=svyHandlers",
			api : "=svyApi",
			svyServoyapi : "="
		},
		link : function($scope, $element, $attrs) {
			if($attrs['svyPortalCell']) {
				$animate.enabled($element, false);
			}
			var child = $element.children();
			var ngModel = child.controller("ngModel");
			var isDataFormatted = true;

			var options = {
					widgetParent: $(document.body),
					useCurrent : false,
					useStrict : true,
					showClear : true,
					ignoreReadonly : true,
					showTodayButton: true,
					calendarWeeks: true,
					showClose: true,
					icons: {
						close: 'glyphicon glyphicon-ok'
					}
				};
			// rely on servoy_app.js setLocale which searches the correct locale to set
			options.locale = numeral.locale();
			var showISOWeeks = $applicationService.getUIProperty('ngCalendarShowISOWeeks');
			if (showISOWeeks)
			{
				options.isoCalendarWeeks = true;
			}	
			
			child.datetimepicker(options);
			
			var theDateTimePicker = child.data('DateTimePicker');
			//this method sets locale tooltips for the buttons in the datepickers
			$utils.getI18NCalendarMessages(theDateTimePicker);
					
			function inputChanged(e) {
				if ($scope.model.findmode) {
					ngModel.$setViewValue(child.children("input").val());
				} else {
					if (e.date)
					{
						if (!$scope.model.dataProviderID && $scope.model.format.display)
						{
							// if format has no seconds do not use the current seconds, use 0; otherwise when parsing the input value seconds will be lost anyway
							if ($scope.model.format.display.indexOf('s') < 0)
							{
								e.date.seconds(0);
							}
							if ($scope.model.format.display.indexOf('m') < 0)
							{
								e.date.minutes(0);
							}
							if ($scope.model.format.display.indexOf('h') < 0 && $scope.model.format.display.indexOf('H') < 0)
							{
								e.date.hours(0);
							}
							
						}	
						ngModel.$setViewValue(e.date.toDate());
					}	
					else
						ngModel.$setViewValue(null);
				}
				ngModel.$setValidity("", true);
				$scope.svyServoyapi.apply('dataProviderID');
			};
			
			function findModeEventListener(event) {
				if(event.keyCode === 13) {
					inputChanged(event);
					event.stopPropagation();
					setTimeout(function() {
						// Create a new keyboard event with the "Enter" key
						var enterKeyEvent = new KeyboardEvent("keydown", {
							key: "Enter",
							code: "Enter",
							which: 13,
							keyCode: 13,
							bubbles: true,
							cancelable: true
						});
						// Dispatch the event on the target element
						$element.get(0).dispatchEvent(enterKeyEvent);
					}, 0);
				}
			};

			function correctDateValueToUse(newValue) {
				// .date() throws exception if (newDate !== null && typeof newDate !== 'string' && !moment.isMoment(newDate) && !(newDate instanceof Date))
				// because we do call this method quite fast using ngModel$viewValue that value might be NaN (used by angular JS) because ngModel code only
				// initializes it to the actual value in a watch so later; we also want to be able to call .date() for undefined values
				return (angular.isDefined(newValue) && !isNaN(newValue)) ? newValue : null;
			}

			// when model change, update our view, set the date in the
			// datepicker
			ngModel.$render = function() {
				try {
					$element.off("dp.change", inputChanged);
					if (theDateTimePicker && !$scope.model.findmode) theDateTimePicker.date(correctDateValueToUse(ngModel.$viewValue)); // set default date for widget open; turn undefined to null as well (undefined gives exception)
					else {
						// in find mode
						child.children("input").val(ngModel.$viewValue);
					}
				} finally {
					$element.on("dp.change", inputChanged);
				}
			};

			var dateFormat = 'YYYY-MM-DD';

			/**
			 * detect IE returns true if browser is IE or false, if browser is
			 * not Internet Explorer
			 */
			function detectIE() {
				var ua = window.navigator.userAgent;

				var msie = ua.indexOf('MSIE ');
				var trident = ua.indexOf('Trident/');
				var edge = ua.indexOf('Edge/');
				if (msie > 0 || trident > 0 || edge > 0) {
					return true;
				}

				// other browser
				return false;
			}

			// helper function
			function setDateFormat(format, which) {
				if (!isDataFormatted)
					return;
				$element.off("dp.change", inputChanged);
				if (format && format[which]) {
					dateFormat = moment().toMomentFormatString(format[which]);
				}
				
				if ($scope.model.format && $scope.model.format.isMask)
				{
					// delete shortcut clears the date; this interferes(behaves strange) with mask, so cancel the shortcut in this scenario 
					var defaultBinding = theDateTimePicker.keyBinds();
					defaultBinding.delete = function (widget) {
						// nop
			        }
				}
                var editFormat = $scope.model.format ?  ($scope.model.format.edit ? $scope.model.format.edit : $scope.model.format.display ): null;
                if (editFormat && editFormat.indexOf('MMM') >= 0)
                {
                    // disable today shortcut if month appears as text when editing 
                    var defaultBinding = theDateTimePicker.keyBinds();
                    defaultBinding.t = function (widget) {
                        // nop
                    }
                }
                
				if (angular.isDefined(theDateTimePicker)) { // can be undefined in find mode
					var ieVersion = detectIE();
					var start=0;
					var end=0;
					if(ieVersion){

						start = child.children("input")[0].selectionStart;
						end = child.children("input")[0].selectionEnd;
					}
					theDateTimePicker.format(dateFormat);

					try {
						theDateTimePicker.date(correctDateValueToUse(ngModel.$viewValue));

					}
					finally {
						var elem = document.getElementById($scope.model.svyMarkupId);
						if(start > 0){
							if (elem.setSelectionRange) {
								isDataFormatted = true;
								elem.setSelectionRange(start, end);
								isDataFormatted = false;
							} else if (elem.createTextRange) {
							isDataFormatted = true;
							var selRange = elem.createTextRange();
							selRange.collapse(true);
							selRange.moveStart('character', start);
							selRange.moveEnd('character', end);
							selRange.select();
							isDataFormatted = false;
						} else if (typeof start != 'undefined') {
							isDataFormatted = true;
							elem.selectionStart = start;
							elem.selectionEnd = end;
							isDataFormatted = false;
							}
						}
						$element.on("dp.change",inputChanged);
					}
				}
			}

			$element.on("dp.change", inputChanged);

			function onError(val) {
				if (child.children("input").val() === '') {
					ngModel.$setViewValue(null);
					ngModel.$setValidity("", true);
					$scope.svyServoyapi.apply('dataProviderID');
					return;
				} else if (!moment(child.children("input").val()).isValid) {
					ngModel.$setValidity("", false);
				}
				$scope.$digest();
			}
			$element.on("dp.error", onError);
			
			$scope.$watch('model.readOnly', function() {
				if ($scope.model.readOnly) {
					var opts = {...options};
					opts.ignoreReadonly = false;
					child.datetimepicker("options", opts);
				}
			});

			$scope.$watch('model.findmode', function() {
				if ($scope.model.findmode) {
					theDateTimePicker.destroy();
					child.children("input").on("keydown", findModeEventListener);
				} else {
					child.children("input").off("keydown", findModeEventListener);
					$element.off("dp.error");
					var opts = {
								widgetParent: $(document.body),
								useCurrent : false,
								useStrict : true,
								showClear : true,
								ignoreReadonly : true,
								showTodayButton: true,
								calendarWeeks: true,
								showClose: true,
								icons: {
									close: 'glyphicon glyphicon-ok'
								}
							};
					if (showISOWeeks)
					{
						opts.isoCalendarWeeks = true;
					}	
					child.datetimepicker(opts);
					theDateTimePicker = child.data('DateTimePicker');
					theDateTimePicker.format(dateFormat);
					try {
						$element.off("dp.change", inputChanged);
						theDateTimePicker.date(correctDateValueToUse(ngModel.$viewValue));
					} finally {
						$element.on("dp.error", onError);
						$element.on("dp.change", inputChanged);
					}
				}
			});

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
				} else {
					ngModel.$setValidity("", true);
					if (storedTooltip !== false)
						$scope.model.toolTipText = storedTooltip;
					storedTooltip = false;
				}
			}
			
			$scope.focusGained = function(event) {
				if (!$scope.model.findmode) {
					if ($scope.model.format.edit && $scope.model.format.isMask) {
						var settings = {};
						settings.placeholder = $scope.model.format.placeHolder ? $scope.model.format.placeHolder : " ";
						if ($scope.model.format.allowedCharacters)
							settings.allowedCharacters = $scope.model.format.allowedCharacters;

						$element.find('input').mask($scope.model.format.edit, settings);
						// library doesn't handle well this scenario, forward focus event to make sure mask is set
						if ($element.find('input').val() == '') $element.find('input').trigger("focus.mask");
						$element.on("dp.change", inputChanged);
					}
					else if ($scope.model.format.edit)
						setDateFormat($scope.model.format, 'edit');
					else
						setDateFormat($scope.model.format, 'display');
				}
			}

			$scope.focusLost = function(event) {
				if (!$scope.model.findmode) {
					if ($scope.model.format.edit && $scope.model.format.isMask)
					{
						$element.find('input').unmask();
					}
					else
					{
						setDateFormat($scope.model.format, 'display');
					}
				}
				else
				{
					inputChanged(event);
				}	
			}

			/**
			 * Set the focus to this calendar.
			 * 
			 * @example %%prefix%%%%elementName%%.requestFocus();
			 * @param mustExecuteOnFocusGainedMethod
			 *            (optional) if false will not execute the onFocusGained
			 *            method; the default value is true
			 */
			$scope.api.requestFocus = function(mustExecuteOnFocusGainedMethod) {
				var input = $element.find('input');
				if (mustExecuteOnFocusGainedMethod === false && $scope.handlers.onFocusGainedMethodID) {
					input.unbind('focus');
					input[0].focus();
					input.bind('focus', $scope.handlers.onFocusGainedMethodID)
				} else {
					input[0].focus();
				}
			}

			$scope.api.getWidth = $apifunctions.getWidth($element[0]);
			$scope.api.getHeight = $apifunctions.getHeight($element[0]);
			$scope.api.getLocationX = $apifunctions.getX($element[0]);
			$scope.api.getLocationY = $apifunctions.getY($element[0]);

			var element = $element.children().first();
			var inputElement = element.children().first();
			var className = null;
			Object.defineProperty($scope.model, $sabloConstants.modelChangeNotifier, {
				configurable : true,
				value : function(property, value) {
					switch (property) {
					case "borderType":
						$svyProperties.setBorder(element, value);
						break;
					case "margin":
						if (value)
							element.css(value);
						break;
					case "background":
					case "transparent":
						$svyProperties.setCssProperty(inputElement, "backgroundColor", $scope.model.transparent ? "transparent" : $scope.model.background);
						break;
					case "foreground":
						$svyProperties.setCssProperty(inputElement, "color", value);
						break;
					case "selectOnEnter":
						if (value)
							$svyProperties.addSelectOnEnter(inputElement);
						break;
					case "fontType":
						$svyProperties.setCssProperty(inputElement, "font", value);
						break;
					case "enabled":
						if (value)
							inputElement.removeAttr("disabled");
						else
							inputElement.attr("disabled", "disabled");
						break;
					case "editable":
						if (value && !$scope.model.readOnly)
							inputElement.removeAttr("readonly");
						else
							inputElement.attr("readonly", "readonly");
						break;
					case "readOnly":
						if (!value && $scope.model.editable)
							inputElement.removeAttr("readonly");
						else
							inputElement.attr("readonly", "readonly");
						break;	
					case "horizontalAlignment":
						$svyProperties.setHorizontalAlignment(inputElement, value);
						break;
					case "rolloverCursor":
						element.css('cursor', value == 12 ? 'pointer' : 'default');
						break;
					case "size":
						$svyProperties.setCssProperty(inputElement, "height", value.height);
						break;
					case "styleClass":
						if (className)
							inputElement.removeClass(className);
						className = value;
						if (className)
							inputElement.addClass(className);
						break;
					case "format":
						setDateFormat($scope.model.format, 'display');
						break;
					}
				}
			});
			$svyProperties.createTooltipState(element, function() { return $scope.model.toolTipText });
			var destroyListenerUnreg = $scope.$on("$destroy", function() {
				if (angular.isDefined(theDateTimePicker)) { // can be undefined in find mode
					theDateTimePicker.destroy();
				}
				destroyListenerUnreg();
				delete $scope.model[$sabloConstants.modelChangeNotifier];
			});
			// data can already be here, if so call the modelChange function so
			// that it is initialized correctly.
			var modelChangFunction = $scope.model[$sabloConstants.modelChangeNotifier];
			for (var key in $scope.model) {
				modelChangFunction(key, $scope.model[key]);
			}
		},
		templateUrl : 'servoydefault/calendar/calendar.html'
	};
})
