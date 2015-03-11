angular.module('servoydefaultCombobox', ['servoy', 'ui.select'])
.directive('servoydefaultCombobox', ['$timeout', function ($timeout) {
	return {
		restrict: 'E',
		scope: {
			model: "=svyModel",
			api: "=svyApi",
			handlers: "=svyHandlers",
			svyServoyapi: "="
		},
		controller: function ($scope) {
			var minHeight = $scope.model.size.height + 'px';
			$scope.style = {
					'min-height': minHeight,
					height: '100%',
					'min-width': $scope.model.size.width + 'px',
					width: '100%'
			};

			$scope.findMode = false;
		},
		link: function (scope, element, attrs) {

			scope.$watch("model.format", function (newVal) {
				if (newVal && newVal["text-transform"]) {
					scope.style["text-transform"] = newVal["text-transform"];
				}
			});

			scope.api.setValueListItems = function (values) {
				var valuelistItems = [];
				var i = 0;
				var item = {};
				for (i = 0; i < values.length; i++) {
					item = {};
					item.displayValue = values[i][0];
					if (values[i][1] !== undefined) {
						item.realValue = values[i][1];
					}
				}
				scope.model.valuelistID = valuelistItems;
			};

<<<<<<< Upstream, based on branch 'master' of https://github.com/Servoy/servoy-client.git
        var storedTooltip = false;
     	scope.api.onDataChangeCallback = function(event, returnval) {
          var ngModel = element.children().controller("ngModel");
=======
			// special method that servoy calls when this component goes into find mode.
			scope.api.setFindMode = function (findMode, editable) {
				scope.findMode = findMode;
				if (findMode) {
					scope.wasEditable = scope.model.editable;
					if (!scope.model.editable) {scope.model.editable = editable; }
				} else {
					scope.model.editable = scope.wasEditable;
				}
			};

			var storedTooltip = false;
			scope.api.onDataChangeCallback = function(event, returnval) {
				var ngModel = element.children().controller("ngModel");
>>>>>>> b86a9f5 More fixes for SVY-7989 : The application freezes for 1 minute, apparently without any specific reason, no error in console, just on log.
				var stringValue = (typeof returnval === 'string' || returnval instanceof String);
				if (!returnval || stringValue) {
					element[0].focus();
					ngModel.$setValidity("", false);
					if (stringValue) {
						if (storedTooltip === false) { 
							storedTooltip = scope.model.toolTipText; 
						}
						scope.model.toolTipText = returnval;
					}
				}
				else {
					ngModel.$setValidity("", true);
					if (storedTooltip !== false) scope.model.toolTipText = storedTooltip;
					storedTooltip = false;
				}
			};

<<<<<<< Upstream, based on branch 'master' of https://github.com/Servoy/servoy-client.git
        scope.onItemSelect = function (event) {
          $timeout(function () {
            if (scope.handlers.onActionMethodID) {
              scope.handlers.onActionMethodID(event);
            }
            scope.svyServoyapi.apply('dataProviderID');
          }, 0);
        };
      },
      templateUrl: 'servoydefault/combobox/combobox.html'
    };
  }])
  .filter('emptyOrNull', function () {
    return function (item) {
      if (item === null || item === '') {return '&nbsp;'; }
      return item;
    };
  })
  .filter('showDisplayValue', function () { // filter that takes the realValue as an input and returns the displayValue
    return function (input, valuelist) {
      var i = 0;
      var realValue = input;
      if (input && valuelist) {
        if (input.hasOwnProperty("realValue")) {
              realValue = input.realValue;
        }
        //TODO performance upgrade: change the valuelist to a hashmap so that this for loop is no longer needed. 
        //maybe to something like {realValue1:displayValue1, realValue2:displayValue2, ...}
        for (i = 0; i < valuelist.length; i++) {
          if (realValue === valuelist[i].realValue) {
            return valuelist[i].displayValue;
          }
        }
      }
      return input;
    };
  });
=======
			scope.onItemSelect = function (event) {
				$timeout(function () {
					if (scope.handlers.onActionMethodID) {
						scope.handlers.onActionMethodID(event);
					}
					scope.svyServoyapi.apply('dataProviderID');
				}, 0);
			};
		},
		templateUrl: 'servoydefault/combobox/combobox.html'
	};
}])
.filter('emptyOrNull', function () {
	return function (item) {
		if (item === null || item === '') {return '&nbsp;'; }
		return item;
	};
})
.filter('showDisplayValue', function () { // filter that takes the realValue as an input and returns the displayValue
	return function (input, valuelist) {
		var i = 0;
		var realValue = input;
		if (input && valuelist) {
			if (input.hasOwnProperty("realValue")) {
				realValue = input.realValue;
			}
			//TODO performance upgrade: change the valuelist to a hashmap so that this for loop is no longer needed. 
			//maybe to something like {realValue1:displayValue1, realValue2:displayValue2, ...}
			for (i = 0; i < valuelist.length; i++) {
				if (realValue === valuelist[i].realValue) {
					return valuelist[i].displayValue;
				}
			}
		}
		return input;
	};
});
>>>>>>> b86a9f5 More fixes for SVY-7989 : The application freezes for 1 minute, apparently without any specific reason, no error in console, just on log.
