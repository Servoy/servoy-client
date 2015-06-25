	   $scope.getTabAt = function(index) {
    	   if(index > 0 && index <= $scope.model.tabs.length) {
    		   return $scope.model.tabs[index - 1];
    	   }
       }

       
	// the api defined in the spec file
       $scope.api.addTab = function(form, nameArg, tabText, tooltip, iconURL, fg, bg, relation, index) {
    	   if (!$scope.model.tabs) $scope.model.tabs = [];
    	   var insertPosition = (index == undefined) ? $scope.model.tabs.length : ((index == -1 || index > $scope.model.tabs.length) ? $scope.model.tabs.length : index);
    	   for(var i = $scope.model.tabs.length; i > insertPosition; i--) {
    		   $scope.model.tabs[i] = $scope.model.tabs[i - 1]; 
    	   }
    	   if (!tabText) {
    		   if (nameArg) tabText = nameArg;
    		   else {
    			   if (typeof form == 'string')
    			   {
    				   tabText = form;
    			   }
    			   else if (form)
    			   {
    				   tabText = form._formname_;
    			   }   
    			  
    		   }
    	   }
    	   $scope.model.tabs[insertPosition] = {
    			   name: nameArg,
    			   containsFormId: form,
    			   text: tabText,
    			   relationName: relation,
    			   active: false,
    			   disabled: false,
    			   foreground: fg };
    	   if ($scope.model.tabs.length == 1 || !$scope.model.tabIndex)
    	   {
//    		   java.lang.System.out.println(new Date().getTime() + " : tabIndex = 1 (server side add first tab or tabIndex previously undefined); " + $scope.model.tabIndex);
    		   $scope.model.tabIndex = 1; 
    	   }
    	   else if ($scope.model.tabIndex > insertPosition) // here $scope.model.tabIndex should always be defined...
    	   {
    		   $scope.model.tabIndex++;  
//    		   java.lang.System.out.println(new Date().getTime() + " : tabIndex = " + $scope.model.tabIndex + " insert " + insertPosition + " (server side add before current tabIndex)");
    	   }	   
    	   
    	   return true;
       }
       
       $scope.api.removeTabAt = function(index) {
    	   if(index > 0 && index <= $scope.model.tabs.length) {
    		   
    		   if ($scope.model.tabs[index] === $scope.model.selectedTab)
    			   delete $scope.model.selectedTab;
    		   
        	   for(var i = index - 1; i < $scope.model.tabs.length - 1; i++) {
        		   $scope.model.tabs[i] = $scope.model.tabs[i + 1];
        	   }
        	   $scope.model.tabs.length = $scope.model.tabs.length - 1;
        	   if ($scope.model.tabIndex >= index)
        	   {
        		   if ($scope.model.tabIndex === index)
        		   {
        			   $scope.model.tabIndex = 1;
        		   }  
        		   else
        		   {
        			   $scope.model.tabIndex--;
        		   }   
        	   }   
    		   //java.lang.System.out.println(new Date().getTime() + " : tabIndex = " + $scope.model.tabIndex + " (server side removeTabAt)");
        	   return true;
    	   }
    	   return false;
       }
       
       $scope.api.removeAllTabs = function() {
    	   if($scope.model.tabs.length > 0) {
    		   $scope.model.tabs.length = 0;
    		   $scope.model.tabIndex = -1;
    		   delete $scope.model.selectedTab;
//    		   java.lang.System.out.println(new Date().getTime() + " : tabIndex = " + $scope.model.tabIndex + " (removeAllTabs)");
    		   return true;
    	   }
    	   return false;
       }
       
       $scope.api.getMaxTabIndex = function() {
    	   return $scope.model.tabs.length; 
       }
       
       $scope.api.getSelectedTabFormName = function() {
    	   var selectedTab = $scope.getTabAt($scope.model.tabIndex); 
    	   return selectedTab ? selectedTab.containsFormId : null;
       }
                
       $scope.api.getTabBGColorAt = function(index) {
    	   return null;
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
    	   var tab = $scope.getTabAt(index);
    	   return tab ? tab.text  : '';
       }
       
       $scope.api.isTabEnabled = function(index) {
    	   return $scope.api.isTabEnabledAt(index);
       }
                
       $scope.api.isTabEnabledAt = function(index) {
    	   var tab = $scope.getTabAt(index);
    	   return tab ? (tab.disabled == undefined ? true : !tab.disabled) : true;
       }
        
       $scope.api.setTabBGColorAt = function(index, bgcolor) {
       }

       $scope.api.setTabEnabled = function(index, enabled) {
    	   $scope.api.setTabEnabledAt(index, enabled);
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
    	   var tab = $scope.getTabAt(index);
    	   if(tab) {
    		   tab.text = text;
    	   }    	    	   
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
    	   return 'TABPANEL';
       }
       
       $scope.api.getName = function() {
    	   return $scope.model.name;
       }