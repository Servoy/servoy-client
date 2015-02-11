angular.module('bootstrapcomponentsChoicegroup',['servoy']).directive('bootstrapcomponentsChoicegroup', function($utils) {  
    return {
      restrict: 'E',
      scope: {
        name: "=",
        model: "=svyModel",
        handlers: "=svyHandlers",
        api: "=svyApi",
        svyServoyapi: "="
      },
      link: function($scope, $element, $attrs) {
          
    	  $scope.selection= []
    	  
          $scope.$watch('model.dataProviderID', function() { 
             setSelectionFromDataprovider();
          })
          $scope.$watch('model.valuelistID',function() {
            if(!$scope.model.valuelistID) return; // not loaded yet
            setSelectionFromDataprovider();
          })
          
          $scope.itemClicked = function($event,$index){
           
    		if ($scope.model.inputType == 'radio')
    		{
    			$scope.model.dataProviderID = $scope.model.valuelistID[$index].realValue;
    		}
    		else
    		{
    			$scope.model.dataProviderID = getDataproviderFromSelection()
    		}	
            
            $scope.svyServoyapi.apply('dataProviderID')        
          }

    	  function setSelectionFromDataprovider(){
    		  if(!$scope.model.dataProviderID) return
    		  $scope.selection =[]
    		  var arr = $scope.model.dataProviderID.split('\n')
    		  arr.forEach(function(element, index, array){
    			  for(var i=0;i<$scope.model.valuelistID.length;i++){
    				  var item= $scope.model.valuelistID[i];
    				  if(item.realValue && item.realValue==element) 
    				  {
    					  if ($scope.model.inputType == 'radio')
    					  {
    						  $scope.selection[i] = $scope.model.dataProviderID;
    					  }
    					  else
    					  {
    						  $scope.selection[i] = true;
    					  }
    				  }
    			  }
    		  });
    	  }

    	  function getDataproviderFromSelection(){
    		  var ret ="";
    		  $scope.selection.forEach(function(element, index, array){
    			  if(element == true) ret+= $scope.model.valuelistID[index].realValue+'\n';
    		  });
    		  if(ret === "") ret =null
    		  return ret;
    	  }
      },
      templateUrl: 'bootstrapcomponents/choicegroup/choicegroup.html'
    };
  })

  
  
  
  
