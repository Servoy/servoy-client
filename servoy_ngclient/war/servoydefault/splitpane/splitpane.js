angular.module('servoydefaultSplitpane',['servoy']).directive('servoydefaultSplitpane', function() {  
    return {
      restrict: 'E',
      transclude: true,
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
    	  
    	  $scope.$watch("model.readOnly", function(newValue) {
    		  if ($scope.model.tabs[0] && $scope.model.tabs[0].containsFormId)
    		  {
    			  $scope.svyServoyapi.setFormReadOnly($scope.model.tabs[0].containsFormId,newValue);
    		  }
    		  if ($scope.model.tabs[1] && $scope.model.tabs[1].containsFormId)
    		  {
    			  $scope.svyServoyapi.setFormReadOnly($scope.model.tabs[1].containsFormId,newValue);
    		  }
    	  });
    	  $scope.$watch("model.enabled", function(newValue) {
    		  if ($scope.model.tabs[0] && $scope.model.tabs[0].containsFormId)
    		  {
    			  $scope.svyServoyapi.setFormEnabled($scope.model.tabs[0].containsFormId,newValue);
    		  }
    		  if ($scope.model.tabs[1] && $scope.model.tabs[1].containsFormId)
    		  {
    			  $scope.svyServoyapi.setFormEnabled($scope.model.tabs[1].containsFormId,newValue);
    		  }
    	  });
    	  $scope.$watch("model.size.height", function(newValue, oldValue) {
    		 if($scope.model.tabOrientation == -3) {
    			 var delta = newValue - oldValue;
    			 if(delta != 0) {
    				 $scope.api.setDividerLocation(($scope.api.getDividerLocation() - delta) + Math.round(delta * $scope.resizeWeight));
    			 }
    		 } 
    	  });
    	  $scope.$watch("model.size.width", function(newValue, oldValue) {
     		 if($scope.model.tabOrientation == -2) {
     			 var delta = newValue - oldValue;
     			 if(delta != 0) {
     				$scope.api.setDividerLocation(($scope.api.getDividerLocation() - delta) + Math.round(delta * $scope.resizeWeight)); 
     			 }     				 
     		 }
     	  });
    	  
    	  if ($scope.model.tabs && $scope.model.tabs[0] && $scope.model.tabs[0].containsFormId) {
    		  $scope.svyServoyapi.showForm($scope.model.tabs[0].containsFormId, $scope.model.tabs[0].relationName,0);
    	  }
    	  if ($scope.model.tabs && $scope.model.tabs[1] && $scope.model.tabs[1].containsFormId) {
    		  $scope.svyServoyapi.showForm($scope.model.tabs[1].containsFormId, $scope.model.tabs[1].relationName,1);
    	  }
    	  $scope.onChange = function() {
    		  if($scope.handlers.onChangeMethodID) $scope.handlers.onChangeMethodID(-1,event);
    	  }
    	  
          $scope.getForm = function(tab) {
        	  return $scope.svyServoyapi.getFormUrl(tab.containsFormId);
          }
          
          // the api defined in the spec file
          $scope.api.addTab = function(form, nameArg, tabText, tooltip, iconURL, fg, bg, relation, index) {
       	   return false;
          }
          
          $scope.api.removeTabAt = function(index) {
       	   return false;
          }
          
          $scope.api.removeAllTabs = function() {
       	   return false;
          }
          
          $scope.api.getMaxTabIndex = function() {
       	   return 1; 
          }
           
          $scope.api.getTabFGColorAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? tab.foreground  : '';
          }
          
          $scope.api.getTabFormNameAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? tab.containsFormId  : '';
          }

          $scope.api.getTabNameAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? tab.name  : '';
          }

          $scope.api.getTabRelationNameAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? tab.relationName  : '';
          }
          
          $scope.api.getTabTextAt = function(index) {
       	   return null;
          }
                   
          $scope.api.isTabEnabledAt = function(index) {
       	   var tab = $scope.getTabAt(index);
       	   return tab ? (tab.disabled == undefined ? true : !tab.disabled) : true;
          }

          $scope.api.setTabEnabledAt = function(index, enabled) {
       	   var tab = $scope.getTabAt(index);
       	   if(tab) {
       		   tab.disabled = !enabled;
       	   }
          }

          $scope.api.setTabFGColorAt = function(index, fgcolor) {
       	   var tab = $scope.getTabAt(index);
       	   if(tab) {
       		   tab.foreground = fgcolor;
       	   }    	   
          }

          $scope.api.setTabTextAt = function(index, text) {    	   
          }

          $scope.api.getHeight = function() {
       	   return $scope.model.size.height;
          }
          
          $scope.api.getLocationX = function() {
       	   return $scope.model.location.x;
          }
          
          $scope.api.getLocationY = function() {
       	   return $scope.model.location.y;
          }

          $scope.api.getWidth = function() {
       	   return $scope.model.size.width;
          }
          
          $scope.api.setLocation = function(x, y) {
       	   $scope.model.location.x = x;
       	   $scope.model.location.y = y;
          }
          
          $scope.api.setSize = function(width, height) {
       	   $scope.model.size.width = width;
       	   $scope.model.size.height = height;
          }
          
          $scope.api.getElementType= function() {
       	   return 'SPLITPANE';
          }
          
          $scope.api.getName = function() {
       	   return $scope.model.name;
          }
          
          $scope.api.setLeftForm = function(form, relation){
       	      $scope.model.tabs[0] = {
    			   name: null,
    			   containsFormId: form,
    			   text: null,
    			   relationName: relation,
    			   active: false,
    			   disabled: false,
    			   foreground: null };
       	      $scope.svyServoyapi.showForm($scope.model.tabs[0].containsFormId, $scope.model.tabs[0].relationName,0);
        	  return true;
          }
          
          $scope.api.setRightForm = function(form, relation) {
       	      $scope.model.tabs[1] = {
       			   name: null,
       			   containsFormId: form,
       			   text: null,
       			   relationName: relation,
       			   active: false,
       			   disabled: false,
       			   foreground: null };        	  
       	      $scope.svyServoyapi.showForm($scope.model.tabs[1].containsFormId, $scope.model.tabs[1].relationName,0);        	  
        	  return true;
          }
          
          $scope.api.getLeftForm = function() {
        	  return $scope.model.tabs[0].containsFormId;
          }
          
          $scope.api.getRightForm = function() {
        	  return $scope.model.tabs[1].containsFormId;
          }
          
          $scope.api.getContinuousLayout = function() {
        	  return true;
          }
          
          $scope.api.setContinuousLayout = function(b) {
          }
          
          $scope.api.getDividerLocation = function() {
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
          
          $scope.api.setDividerLocation = function(location) {
        	 if(location >= 0 ) {
	  			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
	 			 var pane1 = angular.element($element[0].querySelector(".split-pane1"));
	 			 var pane2 = angular.element($element[0].querySelector(".split-pane2"));
	
	 			 var pos = location;
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
        	 }
          }
          
          $scope.api.getDividerSize = function() {
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
          
          $scope.api.setDividerSize = function(size) {
        	 if(size >= 0) {
	 			 var dividerEl = angular.element($element[0].querySelector(".split-handler"));
				 if($scope.model.tabOrientation == -3) {
					 dividerEl.css('height', size + 'px'); 
				 }
				 else {
					 dividerEl.css('width', size + 'px'); 
				 }
        	 }
          } 
          
          $scope.api.getResizeWeight = function() {
        	  return $scope.resizeWeight;
          }
          
          $scope.api.setResizeWeight = function(resizeW) {
        	  $scope.resizeWeight = resizeW;
          }  
          
          $scope.api.getLeftFormMinSize = function() {
        	  return $scope.pane1MinSize;
          }
          
          $scope.api.setLeftFormMinSize = function(minSize) {
        	  $scope.pane1MinSize = minSize;
          } 
          
          $scope.api.getRightFormMinSize = function() {
        	  return $scope.pane2MinSize;
          }
          
          $scope.api.setRightFormMinSize = function(minSize) {
        	  $scope.pane2MinSize = minSize;
          }           
      },
      templateUrl: 'servoydefault/splitpane/splitpane.html',
      replace: true
    };
  })