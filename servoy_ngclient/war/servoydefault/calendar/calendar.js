angular.module('svyCalendar',['servoy']).directive('svyCalendar', function(dateFilter,$log) {  
    return {
      restrict: 'E',
      transclude: true,
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
          })

          //convert from UI input to servoy model
          $scope.pushChange = function (d){
              // test for prevInputType == findmode if that is set
              if (!findMode) {  
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
            	if(format.display.match(/.*y.*/gi) != null){
                    //contains years
            		if( format.display.match(/.*H+.*m+.*/gi) != null){
            			dateFormat = "YYYY-MM-DD hh:mm a"    //use angularjs date filter formats to convert it to the required browser format
                        hasDate = true;
                        hasTime = true;
                    }
            	} else if(format.display.match(/.*H+.*m+.*/gi) != null){
            		dateFormat ='hh:mm a'
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
          
          var findMode = false;
          // special method that servoy calls when this component goes into find mode.
          $scope.api.setFindMode = function(mode, editable) {
        	findMode = mode;
        	if (findMode)
      	 	{
      	 		$scope.wasEditable = $scope.model.editable;
      	 		if (!$scope.model.editable) $scope.model.editable = editable;
      	 	}
      	 	else
      	 	{
      	 		$scope.model.editable = $scope.wasEditable;
      	 	}
          };
      },
      templateUrl: 'servoydefault/calendar/calendar.html',
      replace: true
    };
  })