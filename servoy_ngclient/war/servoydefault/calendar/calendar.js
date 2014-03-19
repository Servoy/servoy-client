servoyModule.directive('svyCalendar', function(dateFilter,$log) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
      	model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
          $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
          
          $scope.editModel =''; // use string edit model 
          $scope.inputType = 'date';
          var dateFormat = 'yyyy-MM-dd';
          
          $scope.$watch('model.format', function(){
              setInputType($scope.model.format);
          })
          
          //convert from servoy model to ui input
          $scope.$watch('model.dataProviderID', function(){
             if($scope.model.dataProviderID && !($scope.model.dataProviderID instanceof Date)) {$log.error("calendar expects it's dataprovider as Date");}
            $scope.editModel = dateFilter($scope.model.dataProviderID,dateFormat);
          })
          

          
          //convert from UI input to servoy model
          $scope.pushChange = function (){
              // test for prevInputType == findmode if that is set
              if (!prevInputType) {
                var date=null;
                if($scope.editModel !=null && $scope.editModel !='') {
                 var editDate = new Date($scope.editModel);
                 date = new Date(editDate.getTime()+editDate.getTimezoneOffset()*60*1000)
                }
            	$scope.model.dataProviderID = date
              } else {
                $scope.model.dataProviderID = $scope.editModel;
              }
            $scope.handlers.svy_apply('dataProviderID');
          }
          
    //helper function
          function setInputType(format){
           if(!format  || !format.display) return
           if(format.display.match(/.*y.*/gi) != null){
                //contains years
                if( format.display.match(/.*H+.*m+.*/gi) != null){
                    $scope.inputType = 'datetime-local'
                    dateFormat = "yyyy-MM-ddTHH:mm"    //use angularjs date filter formats to convert it to the required browser format
                    
                }else{
                    $scope.inputType = 'date'
                    dateFormat = 'yyyy-MM-dd'
                }
              }else if(format.display.match(/.*H+.*m+.*/gi) != null){
                    $scope.inputType = "time"
                    dateFormat ='HH:mm:ss'
              }else{
                 $scope.inputType = 'date'
                 dateFormat = 'yyyy-MM-dd'
              }
          }
          
          var prevInputType = null;
           // special method that servoy calls when this component goes into find mode.
    	 $scope.api.setFindMode = function(findMode) {
    	 	$log.error("findmode on calendar " + findMode);
    	 	if (findMode) {
    	 		prevInputType = $scope.inputType;
    	 		$scope.inputType = "text";
    	 	}
    	 	else {
    	 		$scope.inputType = prevInputType;
    	 		prevInputType = null;
    	 	}
    	 };
          
      },
      templateUrl: 'servoydefault/calendar/calendar.html',
      replace: true
    };
  })

  
  
  
  
