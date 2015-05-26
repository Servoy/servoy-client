angular.module('bootstrapcomponentsCalendar',['servoy']).directive('bootstrapcomponentsCalendar', function($log) {  
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

			child.datetimepicker();

			$scope.$watch('model.format', function(){
				setDateFormat($scope.model.format);
			})

			function inputChanged(e) {
				if (e.date) ngModel.$setViewValue(e.date.toDate());
				else ngModel.$setViewValue(null);
				ngModel.$setValidity("", true);
				$scope.svyServoyapi.apply('dataProviderID');
			}

			// when model change, update our view, set the date in the datepicker
			ngModel.$render = function() {
				try {
					$element.off("dp.change",inputChanged);
					var x = child.data('DateTimePicker');
					if (x) x.date(angular.isDefined(ngModel.$viewValue) ? ngModel.$viewValue : null); // set default date for widget open; turn undefined to null as well (undefined gives exception)
					else {
						// in find mode 
						child.children("input").val(ngModel.$viewValue);
					}
				} finally {
					$element.on("dp.change",inputChanged);
				}
			};

			var dateFormat = 'YYYY-MM-DD';

			// helper function
			function setDateFormat(format){
				if(format && format.display){
					dateFormat = moment().toMomentFormatString(format.display);
				}
				var x = child.data('DateTimePicker');
				if (angular.isDefined(x)) { // can be undefined in find mode
					x.format(dateFormat);
					try {
						$element.off("dp.change",inputChanged);
						x.date(angular.isDefined(ngModel.$viewValue) ? ngModel.$viewValue : null);
					}
					finally {
						$element.on("dp.change",inputChanged);
					}
				}
			}

			$element.on("dp.change",inputChanged);

			$element.on("dp.error",function(){
				ngModel.$setValidity("", false);
				$scope.$digest();
			});
			
		},
		templateUrl: 'bootstrapcomponents/calendar/calendar.html'
	};
})
