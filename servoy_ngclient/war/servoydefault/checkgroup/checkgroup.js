angular.module('svyCheckgroup',['servoy']).directive('svyCheckgroup', function($utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {
         $scope.notNullOrEmpty = $utils.notNullOrEmpty  // adding it to the root scope doesn't fix the resolution of the comparator in the filter (in this directive). it has to be in local scope. TODO remove the need for this
         $scope.style = {width:'100%',height:'100%'}
         angular.extend($scope.style ,$utils.getScrollbarsStyleObj($scope.model.scrollbars));

          var allowNullinc=0;
          
          $scope.selection= []
          
          $scope.$watch('model.dataProviderID', function() { 
             setSelectionFromDataprovider();
          })
          $scope.$watch('model.valuelistID',function() {
            if(!$scope.model.valuelistID) return; // not loaded yet
            if(isValueListNull($scope.model.valuelistID[0])) allowNullinc=1;
            setSelectionFromDataprovider();
          })
          
          $scope.checkBoxClicked = function($index){
             var checkedTotal = 0;
             for(var i=0;i< $scope.selection.length ;i++){
            	 if($scope.selection[i]==true) checkedTotal++;            	 
             }
            // prevent unselection of the last element if 'allow null' is not set                                          
            if(checkedTotal==0 && allowNullinc ==0){
               $scope.selection[$index] = true;
            }
            $scope.model.dataProviderID = getDataproviderFromSelection()
            
            if(checkedTotal==0 && allowNullinc ==0) return;// only push if it was actualy changed
            $scope.handlers.svy_apply('dataProviderID')
          }
          
          $scope.api.setScroll = function(x, y) {
         	 $element.scrollLeft(x);
         	 $element.scrollTop(y);
          }
          
          $scope.api.getScrollX = function() {
         	 return $element.scrollLeft();
          }
          
          $scope.api.getScrollY = function() {
         	 return $element.scrollTop();
          }
        
          $scope.api.setValueListItems = function(values) 
          {
        	  var valuelistItems = [];
        	  for (var i = 0; i < values.length; i++)
        	  {
        		  var item = {};
        		  item['displayValue'] = values[i][0];
        		  if (values[i][1] !== undefined)
        		  {
        			  item['realValue'] = values[i][1];
        		  }
        		  valuelistItems.push(item); 
        	  }
        	  $scope.model.valuelistID = valuelistItems;
          }
          
         // special method that servoy calls when this component goes into find mode.
       	 $scope.api.setFindMode = function(findMode, editable) {
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
          
    /* helper functions*/
          function setSelectionFromDataprovider(){
            if(!$scope.model.dataProviderID) return
            $scope.selection =[]
            var arr = $scope.model.dataProviderID.split('\n')
            arr.forEach(function(element, index, array){
                for(var i=0;i<$scope.model.valuelistID.length;i++){
                  var item= $scope.model.valuelistID[i];
                    if(item.realValue && item.realValue==element && !isValueListNull(item)) $scope.selection[i-allowNullinc] = true;
                }
            });
          }
          
          function getDataproviderFromSelection(){
            var ret ="";
            $scope.selection.forEach(function(element, index, array){
               // if(index == array.length-allowNullinc) return;
                if(element == true) ret+= $scope.model.valuelistID[index+allowNullinc].realValue+'\n';
            });
              if(ret =="") ret =null
              return ret;
          }
          
          function isValueListNull(item){
              if(item.realValue == null && item.displayValue=='')
              {return true;}
              else return false
          }
      },
      templateUrl: 'servoydefault/checkgroup/checkgroup.html',
      replace: true
    };
  })

  
  
  
  
