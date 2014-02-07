servoyModule.directive('svyCalendar', function(dateFilter,$log) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
      	model: "=svyModel",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
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
              var date=null;
              if($scope.editModel !=null && $scope.editModel !='') {
                  var editDate = new Date($scope.editModel);
                 date = new Date(editDate.getTime()+editDate.getTimezoneOffset()*60*1000)
              }
            $scope.model.dataProviderID = date
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
          
      },
      templateUrl: 'servoydefault/calendar/calendar.html',
      replace: true
    };
  })

  
  
  
  
