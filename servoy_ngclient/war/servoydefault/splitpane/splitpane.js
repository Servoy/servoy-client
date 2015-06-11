angular.module('servoydefaultSplitpane',['servoy']).directive('servoydefaultSplitpane', function($apifunctions) {  
    return {
      restrict: 'E',
      scope: {
        model: "=svyModel",
        svyServoyapi: "=",
        handlers: "=svyHandlers",
        api: "=svyApi"
      },
      controller: function($scope, $element, $attrs) {
    	  
    	  $scope.pane1MinSize = 30;
    	  $scope.pane2MinSize = 30;
    	  
    	  $scope.resizeWeight = 0;
    	  
    	  $scope.$watch("model.size.height", function(newValue, oldValue) {
    		 if($scope.model.tabOrientation == -3) {
    			 var delta = newValue - oldValue;
    			 if(delta != 0) {
    				 $scope.api.setDividerLocation(($scope.api.getBrowserDividerLocation() - delta) + Math.round(delta * $scope.resizeWeight));
    			 }
    		 } 
    	  });
    	  $scope.$watch("model.size.width", function(newValue, oldValue) {
     		 if($scope.model.tabOrientation == -2) {
     			 var delta = newValue - oldValue;
     			 if(delta != 0) {
     				$scope.api.setDividerLocation(($scope.api.getBrowserDividerLocation() - delta) + Math.round(delta * $scope.resizeWeight)); 
     			 }     				 
     		 }
     	  });
    	  
    	  $scope.$watch('model.divSize', function(newValue, oldValue){
        	  var dividerEl = angular.element($element[0].querySelector(".split-handler"));
        	  if($scope.model.tabOrientation == -3) {
				 dividerEl.css('height', $scope.model.divSize + 'px'); 
        	  }
			  else {
				 dividerEl.css('width',  $scope.model.divSize + 'px'); 
			  }
          });
    	  
    	  //called when the divider location is changed from server side scripting
    	  $scope.$watch('model.divLocation', function(newValue, oldValue){
    		  if (newValue && newValue  !== oldValue) {
	  			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
	 			 var pane1 = angular.element($element[0].querySelector(".split-pane1"));
	 			 var pane2 = angular.element($element[0].querySelector(".split-pane2"));
	
	 			 var pos =  $scope.model.divLocation;;
	 			 if($scope.model.tabOrientation == -3) { 
	 				if(pos < 1) {
	 					pos = $scope.model.size.height * pos;
	 				}
	 				dividerEl.css('top', pos + 'px');
	 	            pane1.css('height', pos + 'px');
	 	            pane2.css('top', pos + 'px');
	 			 }
	 			 else {
	 				if(pos < 1) {
	 					pos = $scope.model.size.width * pos;
	 				}
	 				dividerEl.css('left', pos + 'px');
	 	            pane1.css('width', pos + 'px');
	 	            pane2.css('left', pos + 'px');
	 			 }
	 			 if($scope.handlers.onChangeMethodID) $scope.handlers.onChangeMethodID(-1,$.Event("change"));
    		  }
    	  });
    	  
    	  if ($scope.model.tabs && $scope.model.tabs[0] && $scope.model.tabs[0].containsFormId) {
    		  $scope.svyServoyapi.formWillShow($scope.model.tabs[0].containsFormId, $scope.model.tabs[0].relationName,0);
    	  };
    	  if ($scope.model.tabs && $scope.model.tabs[1] && $scope.model.tabs[1].containsFormId) {
    		  $scope.svyServoyapi.formWillShow($scope.model.tabs[1].containsFormId, $scope.model.tabs[1].relationName,1);
    	  };
    	  //called by bg-splitter when the user changes the divider location with the mouse
    	  $scope.onChange = function() {
    		  $scope.model.divLocation = $scope.api.getBrowserDividerLocation();;
    		  $scope.$digest(); // not in angular so we need a digest that will trigger the watch that will then trigger the handler
    	  }
    	  
          $scope.getForm = function(tab) {
        	  if (!tab) return null;
        	  return $scope.svyServoyapi.getFormUrl(tab.containsFormId);
          }
          
          
          // called by the server side function  getDividerSize
          $scope.api.getBrowserDividerSize = function() {
 			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
 			 var dividerSize;
			 if($scope.model.tabOrientation == -3) {
				 dividerSize = dividerEl.css('height'); 
			 }
			 else {
				 dividerSize = dividerEl.css('width'); 
			 }
			 
			 return dividerSize ? dividerSize.substring(0, dividerSize.length - 2) : 0;
          }
          
          // called by the server side function  getDividerLocation
          $scope.api.getBrowserDividerLocation = function() {
  			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
 			 var dividerLocation;
			 if($scope.model.tabOrientation == -3) {
				 dividerLocation = dividerEl.css('top'); 
			 }
			 else {
				 dividerLocation = dividerEl.css('left'); 
			 }
			 
			 return dividerLocation ? dividerLocation.substring(0, dividerLocation.length - 2) : 0;
          }
          
			/**
			 * Set a relationless or related form as left panel.
			 * @example %%prefix%%%%elementName%%.setLeftForm(forms.orders);
			 * @param form the specified form or form name you wish to add as left panel
			 * @return {Boolean} value indicating if tab was successfully added
			 */
			$scope.api.setLeftForm = function(form, relation) {
				$scope.model.tabs[0] = {
					name : null,
					containsFormId : form,
					text : null,
					relationName : relation,
					active : false,
					disabled : false,
					foreground : null
				};
				$scope.svyServoyapi.formWillShow($scope.model.tabs[0].containsFormId,
						$scope.model.tabs[0].relationName, 0);
				return true;
			}
			
			/**
			 * Set a relationless or related form as right panel.
			 * @example %%prefix%%%%elementName%%.setRightForm(forms.orders);
			 * @param form the specified form or form name you wish to add as right panel
			 * @return {Boolean} value indicating if tab was successfully added
			 */
			$scope.api.setRightForm = function(form, relation) {
				$scope.model.tabs[1] = {
					name : null,
					containsFormId : form,
					text : null,
					relationName : relation,
					active : false,
					disabled : false,
					foreground : null
				};
				$scope.svyServoyapi.formWillShow($scope.model.tabs[1].containsFormId,
						$scope.model.tabs[1].relationName, 1);
				return true;
			}
			
			/**
			 * Returns the left form of the split pane.
			 * @example var leftForm = %%prefix%%%%elementName%%.getLeftForm();
			 * @return {FormScope} left form of the split pane
			 */
			$scope.api.getLeftForm = function() {
				return $scope.model.tabs[0].containsFormId;
			}
			
			/**
			 * Returns the right form of the split pane.
			 * @example var rightForm = %%prefix%%%%elementName%%.getRightForm();
			 * @return {FormScope} right form of the split pane
			 */
			$scope.api.getRightForm = function() {
				return $scope.model.tabs[1].containsFormId;
			}
			
          
    	  $scope.api.getWidth = $apifunctions.getWidth($element[0]);
    	  $scope.api.getHeight = $apifunctions.getHeight($element[0]);
    	  $scope.api.getLocationX = $apifunctions.getX($element[0]);
    	  $scope.api.getLocationY = $apifunctions.getY($element[0]);
      },
      templateUrl: 'servoydefault/splitpane/splitpane.html'
    };
  })