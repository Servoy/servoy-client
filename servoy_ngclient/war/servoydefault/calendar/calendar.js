angular.module('servoydefaultCalendar',['servoy']).directive('servoydefaultCalendar', function($log) {  
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi",
        svyApply: "="
      },
      link: function($scope, $element, $attrs) {
    	  var child = $element.children();
    	  var ngModel = child.controller("ngModel");
         
    	  $scope.style = {width:'100%',height:'100%',overflow:'hidden',paddingTop:'0',paddingBottom:'0'}
          
    	  child.datetimepicker();
          
          $scope.$watch('model.format', function(){
              setDateFormat($scope.model.format);
          })
          
       // when model change, update our view, set the date in the datepicker
          ngModel.$render = function() {
        	  var x = child.data('DateTimePicker');
	          if (x)  x.date(ngModel.$viewValue); // set default date for widget open
	          else {
	        	  // in find mode 
	        	  child.children("input").val(ngModel.$viewValue);
	          }
          };

          var dateFormat = 'YYYY-MM-DD'
          //helper function
          function setDateFormat(format){
        	if(format && format.display){
        		dateFormat = moment().toMomentFormatString(format.display);
        	}
        	var x = child.data('DateTimePicker');
        	x.format(dateFormat);
        	x.date(ngModel.$viewValue);
          }

          $element.on("change.dp",function (e) {
        	  if ($scope.findMode) {
        		  ngModel.$setViewValue(child.children("input").val());        		  
        	  }
        	  else {
	        	  if (e.date) ngModel.$setViewValue(e.date.toDate());
	        	  else ngModel.$setViewValue(null);
        	  }
        	  $scope.svyApply('dataProviderID');
          });
          
      	  $scope.findMode = false;
          // special method that servoy calls when this component goes into find mode.
          $scope.api.setFindMode = function(mode, editable) {
        	$scope.findMode = mode;
        	if ($scope.findMode)
      	 	{
        		child.data('DateTimePicker').destroy();
      	 		$scope.wasEditable = $scope.model.editable;
      	 		if (!$scope.model.editable) $scope.model.editable = editable;
      	 	}
      	 	else
      	 	{
      	 		child.datetimepicker();
      	 		var x = child.data('DateTimePicker');
      	 		x.format(dateFormat);
      	 		console.log(ngModel.$viewValue)
            	x.date(ngModel.$viewValue);
      	 		$scope.model.editable = $scope.wasEditable != undefined ? $scope.wasEditable : editable
      	 	}
          };
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
					$scope.model.toolTipText = storedTooltip;
					storedTooltip = false;
				}
			}
      },
      templateUrl: 'servoydefault/calendar/calendar.html'
    };
  })
