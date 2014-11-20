angular.module('servoydefaultTypeahead',['servoy'])
.directive('servoydefaultTypeahead',['formatFilterFilter', function(formatFilter) {  
    return {
      restrict: 'E',
      require: 'ngModel',
      scope: {
        model: "=svyModel",
        svyApply: "=",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      link: function($scope, $element, $attrs,ngModel) {
    	  $scope.style = {width:'100%',height:'100%',overflow:'hidden'}
    	  $scope.findMode = false;

         $scope.formatLabel = function (model){
        	 var displayFormat = undefined;
    		  var type = undefined;
    		  var displayValue = model;
    		  if ($scope.model.valuelistID)
    		  {
	     		  for (var i=0; i< $scope.model.valuelistID.length; i++) {
	     			  if (model === $scope.model.valuelistID[i].realValue) {
	     				  displayValue = $scope.model.valuelistID[i].displayValue;
	     				  break;
	     			  }
	     		  }
    		  }
    		  if($scope.model.format && $scope.model.format.display) displayFormat = $scope.model.format.display;
    		  if($scope.model.format && $scope.model.format.type) type = $scope.model.format.type;	          		
    		  return formatFilter(displayValue, displayFormat ,type);    	 
         }
          $scope.doSvyApply = function (){
        	  if($element.parent().find('.dropdown-menu').attr('aria-hidden') == "true") {
	    		  if ($scope.model.valuelistID) {
	    			  var hasMatchingDisplayValue = false;
		     		  for (var i=0; i< $scope.model.valuelistID.length; i++) {
		     			  if ($element.val() === $scope.model.valuelistID[i].displayValue) {
		     				 hasMatchingDisplayValue = true;
		     				 break;
		     			  }
		     		  }
		     		  if(!hasMatchingDisplayValue) {
		     			 $scope.model.dataProviderID = null;
		     		  }
	    		  }
	        	  $scope.svyApply('dataProviderID');
        	 }
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
       		$scope.findMode = findMode;
       	 	if (findMode)
       	 	{
       	 		$scope.wasEditable = $scope.model.editable;
       	 		if (!$scope.model.editable) $scope.model.editable = editable;
       	 	}
       	 	else
       	 	{
       	 		$scope.model.editable = $scope.wasEditable != undefined ? $scope.wasEditable : editable;
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
      templateUrl: 'servoydefault/typeahead/typeahead.html',
      replace: true
    };
  }])

  
  
  
  
