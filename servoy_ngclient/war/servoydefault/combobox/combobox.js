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
          height: '100%'
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
          var stringValue = typeof returnval === 'string';
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
            scope.model.toolTipText = storedTooltip;
            storedTooltip = false;
          }
        };

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
        //TODO performance upgrade: change the valuelist to a hashmap so that this for loop is no longer needed. 
        //maybe to something like {realValue1:displayValue1, realValue2:displayValue2, ...}
        for (i = 0; i < valuelist.length; i++) {
          if (input.realValue) {
            realValue = input.realValue;
          }
          if (realValue === valuelist[i].realValue) {
            return valuelist[i].displayValue;
          }
        }
      }
      return input;
    };
  });