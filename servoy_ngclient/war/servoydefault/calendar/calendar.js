angular.module('servoydefaultCalendar',['servoy']).directive('servoydefaultCalendar', function($log, $apifunctions) {  
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			handlers: "=svyHandlers",
			api: "=svyApi",
			svyServoyapi: "="
		},
		link: function($scope, $element, $attrs) {
			var child = $element.children();
			var ngModel = child.controller("ngModel");
			var isDataFormatted = true;

			$scope.style = {width:'100%',height: $scope.model.size.height,overflow:'hidden',paddingTop:'0',paddingBottom:'0',position: 'static'}

			child.datetimepicker({
				useCurrent:false,
				useStrict:true,
				showClear: true,
				ignoreReadonly: true
			});

			$scope.$watch('model.size.height', function(){ 
				if ($scope.model.size != undefined)
				{
					$scope.style.height = $scope.model.size.height;
				}
			});

			$scope.$watch('model.format', function(){
				setDateFormat($scope.model.format, 'display');
			});

			function inputChanged(e) {
				if ($scope.model.findmode) {
					ngModel.$setViewValue(child.children("input").val());        		  
				}
				else {
					if (e.date) ngModel.$setViewValue(e.date.toDate());
					else ngModel.$setViewValue(null);
				}
				ngModel.$setValidity("", true);
				$scope.svyServoyapi.apply('dataProviderID');
			};

			// when model change, update our view, set the date in the datepicker
			ngModel.$render = function() {
				try {
					$element.off("dp.change",inputChanged);
					var x = child.data('DateTimePicker');
					if (x && !$scope.model.findmode) x.date(angular.isDefined(ngModel.$viewValue) ? ngModel.$viewValue : null); // set default date for widget open; turn undefined to null as well (undefined gives exception)
					else {
						// in find mode 
						child.children("input").val(ngModel.$viewValue);
					}
				} finally {
					$element.on("dp.change",inputChanged);
				}
			};

			var dateFormat = 'YYYY-MM-DD';

			/**
			 * detect IE
			 * returns true if browser is IE or false, if browser is not Internet Explorer
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
			function setDateFormat(format, which){
				if (!isDataFormatted) return;
				$element.off("dp.change",inputChanged);
				if(format && format[which]){
					dateFormat = moment().toMomentFormatString(format[which]);
				}
				var x = child.data('DateTimePicker');
				if (angular.isDefined(x)) { // can be undefined in find mode
					var ieVersion = detectIE();
					var start=0;
					var end=0;
					if(ieVersion){
						start = document.getElementById($scope.model.svyMarkupId).selectionStart;
						end =  document.getElementById($scope.model.svyMarkupId).selectionEnd;
					}
					x.format(dateFormat);
					
					try {
						x.date(angular.isDefined(ngModel.$viewValue) ? ngModel.$viewValue : null);
						
					}
					finally {
						var elem =  document.getElementById($scope.model.svyMarkupId);
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
							} else if(typeof start != 'undefined'){
								isDataFormatted = true;
								elem.selectionStart=start;
								elem.selectionEnd = end;
								isDataFormatted = false;
							}
						}
						$element.on("dp.change",inputChanged);
					}
				}
			 }

			$element.on("dp.change",inputChanged);

			function onError(val){
				if (child.children("input").val() === '')
				{
					ngModel.$setViewValue(null);
					ngModel.$setValidity("", true);
					$scope.svyServoyapi.apply('dataProviderID');
					return;
				}else if (!moment(child.children("input").val()).isValid){
					ngModel.$setValidity("", false);
				}
				$scope.$digest();
			}
			$element.on("dp.error", onError);
			
			$scope.$watch('model.findmode', function() {
				if ($scope.model.findmode) 
				{
					child.data('DateTimePicker').destroy();
				} 
				else 
				{
					$element.off("dp.error");
					child.datetimepicker({
						useCurrent:false,
						useStrict:true,
						showClear: true,
						ignoreReadonly: true
					});
					var x = child.data('DateTimePicker');
					x.format(dateFormat);
					try {
						$element.off("dp.change", inputChanged);
						x.date(angular.isDefined(ngModel.$viewValue) ? ngModel.$viewValue : null);
					} finally {
						$element.on("dp.error", onError);
						$element.on("dp.change", inputChanged);
					}
				}
			});

			var storedTooltip = false;
			$scope.api.onDataChangeCallback = function(event, returnval) {
				var stringValue = typeof returnval == 'string'
					if(!returnval || stringValue) {
						$element[0].focus();
						ngModel.$setValidity("", false);
						if (stringValue) {
							if ( storedTooltip == false)
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

			$scope.focusGained = function(event) {
				if($scope.model.format.edit) 
					setDateFormat($scope.model.format, 'edit');
				else
					setDateFormat($scope.model.format, 'display');
			}
			
			$scope.focusLost = function(event) {
				setDateFormat($scope.model.format, 'display');
			}

			/**
			 * Set the focus to this calendar.
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
		templateUrl: 'servoydefault/calendar/calendar.html'
	};
})
