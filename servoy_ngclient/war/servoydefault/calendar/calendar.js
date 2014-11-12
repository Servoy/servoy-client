angular.module('servoydefaultCalendar',['servoy']).directive('servoydefaultCalendar', function(dateFilter,$log) {  
    return {
      restrict: 'E',
      scope: {
      	model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%',overflow:'hidden',paddingTop:'0',paddingBottom:'0'}
          
          $scope.editModel =''; // use string edit model 
          $scope.$watch('model.format', function(){
              setDateFormat($scope.model.format);
          })
          
          //convert from servoy model to ui input
          $scope.$watch('model.dataProviderID', function(){
	        	if($scope.model.dataProviderID && !($scope.model.dataProviderID instanceof Date)) {$log.error("calendar expects it's dataprovider as Date");}
	            $scope.editModel = dateFilter($scope.model.dataProviderID,dateFormat);
            //$element.data('DateTimePicker').setValue($scope.model.dataProviderID); // set default date for widget open
          });

          //convert from UI input to servoy model
          $scope.pushChange = function (d){
              // test for prevInputType == findmode if that is set
              if (!$scope.findMode) {  
            	$scope.model.dataProviderID = d;
              } else {
                $scope.model.dataProviderID = $scope.editModel;
              }
            $scope.handlers.svy_apply('dataProviderID');
          }
          
          var dateFormat = 'yyyy-MM-dd'
          //helper function
          function setDateFormat(format){
        	var hasDate = true;
        	var hasTime = false;
        	if(format && format.display){
        		dateFormat = format.display;
            	if(format.display.match(/.*y.*/gi) != null){
                    //contains years
            		if( format.display.match(/.*H+.*m+.*/gi) != null){
                        hasDate = true;
                        hasTime = true;
                    }
            	} else if(format.display.match(/.*H+.*m+.*/gi) != null){
            		hasDate = false;
            		hasTime = true;
            	}
        	}
        	$($element).datetimepicker({
        		pickDate: hasDate,
        		pickTime: hasTime,
        		format: dateFormat
        	});
          }

          $($element).on("dp.change",function (e) {
        	  $scope.$apply(function(){
        		  $scope.editModel = dateFilter(new Date(e.date._d),dateFormat);
        		  $scope.pushChange(new Date(e.date._d));
        	  })        		        	 
          });
          
          $scope.findMode = false;
          // special method that servoy calls when this component goes into find mode.
          $scope.api.setFindMode = function(mode, editable) {
        	$scope.findMode = mode;
        	if ($scope.findMode)
      	 	{
      	 		$scope.wasEditable = $scope.model.editable;
      	 		if (!$scope.model.editable) $scope.model.editable = editable;
      	 	}
      	 	else
      	 	{
      	 		$scope.model.editable = $scope.wasEditable != undefined ? $scope.wasEditable : editable
      	 	}
          };
          var ngModel = $element.children().controller("ngModel");
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