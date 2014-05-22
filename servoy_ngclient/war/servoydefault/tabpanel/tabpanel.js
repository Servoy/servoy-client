angular.module('svyTabpanel',['servoy']).directive('svyTabpanel', function($window) {  
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
        if($scope.model.tabOrientation == -4) {
             var groupHeaderHeight = 39;
             var activeGroupHeaderHeight = 37;
             var scrollHeight = 15;
             $scope.viewportStyle = {height: ($scope.model.size.height - ($scope.model.tabs.length - 1) * groupHeaderHeight - activeGroupHeaderHeight - scrollHeight) + "px"};
        }
        var selectedTab;
        $scope.bgstyle = {}
       
        $scope.$watch("model.tabIndex", function(newValue) {
        	 if($scope.model.tabIndex == undefined) $scope.model.tabIndex = 1; // default it is 1
        	 var realTabIndex = $scope.model.tabIndex - 1;
        	 for(var i=0;i<$scope.model.tabs.length;i++) {
        	 	if (i == realTabIndex) $scope.model.tabs[i].active = true;
        	 	else $scope.model.tabs[i].active = false;
        	 	$scope.model.tabs[i].disabled = false;
        	 }
        });
       $scope.$watch("model.readOnly", function(newValue) {
    	   var activeForm = $scope.getActiveTab()
    	   if (activeForm)
    	   {
    		   $scope.svyServoyapi.setFormReadOnly(activeForm,newValue);
    	   }
       });
       $scope.$watch("model.enabled", function(newValue) {
    	   var activeForm = $scope.getActiveTab()
    	   if (activeForm)
    	   {
    		   $scope.svyServoyapi.setFormEnabled(activeForm,newValue);
    	   }
       });
       $scope.getTemplateUrl = function() {
    	   if ($scope.model.tabOrientation == -1) return "servoydefault/tabpanel/tablesspanel.html";
    	   else if($scope.model.tabOrientation == -4) return "servoydefault/tabpanel/accordionpanel.html"
    	   else return "servoydefault/tabpanel/tabpanel.html";
       }
       $scope.getActiveTab = function() {
    	   for(var i=0;i<$scope.model.tabs.length;i++) {
    		   if ($scope.model.tabs[i].active) {
    			   if (selectedTab != $scope.model.tabs[i])
    			   {
    			   	$scope.select($scope.model.tabs[i]);
    			   } 
    			   break;
    		   }
    	   }
    	   return selectedTab?$scope.svyServoyapi.getFormUrl(selectedTab.containsFormId):"";
       }
       
       $scope.getSelectedTab = function() {
    	   return selectedTab;
       }
       
       $scope.getTabAt = function(index) {
    	   if(index > 0 && index <= $scope.model.tabs.length) {
    		   return $scope.model.tabs[index - 1];
    	   }
       }
       
       $scope.getForm = function(tab) {
       	if (selectedTab && tab.containsFormId == selectedTab.containsFormId) {
       		return $scope.svyServoyapi.getFormUrl(tab.containsFormId);
       	}
       	return "";
       }
       
       function setFormVisible(tab,event) {
       	var promise = $scope.svyServoyapi.setFormVisibility(tab.containsFormId,true, tab.relationName);
       	promise.then(function(ok) {
       		if (ok){
       			if(selectedTab != tab && $scope.handlers.onChangeMethodID)
       			{
       				$scope.handlers.onChangeMethodID($scope.getTabIndex(selectedTab),event instanceof MouseEvent ? event : null);
       			}   			
       			selectedTab = tab;
       			$scope.model.tabIndex = $scope.getTabIndex(selectedTab);
       		} else {
       			// will this ever happen?
       		}
       	});
       }

       $scope.getTabIndex = function(tab) {
    	   if(tab) {
	    	   for(var i=0;i<$scope.model.tabs.length;i++) {
	    		   if ($scope.model.tabs[i].containsFormId == tab.containsFormId) {
	    			   return i + 1;
	    		   }
	    	   }
    	   }
    	   return -1;
       }

       $scope.select = function(tab) {
    	if ((tab != undefined && selectedTab != undefined && tab.containsFormId == selectedTab.containsFormId) || (tab == selectedTab)) return;
    	var selectEvent = $window.event ? $window.event : null;
        if (selectedTab) {
        	var promise =  $scope.svyServoyapi.setFormVisibility(selectedTab.containsFormId,false);
        	promise.then(function(ok) {
        		if (ok) {
        			setFormVisible(tab,selectEvent);
        		}
        		else {
        			tab.active = false;
        			selectedTab.active = true;
        		}
        	})
        }
        else {
        	setFormVisible(tab, selectEvent);
        }
       }
       
       // the api defined in the spec file
       $scope.api.addTab = function(form, nameArg, tabText, tooltip, iconURL, fg, bg, relation, index) {
    	   var insertPosition = (index == undefined) ? $scope.model.tabs.length : ((index == -1 || index > $scope.model.tabs.length) ? $scope.model.tabs.length : index);
    	   for(var i = $scope.model.tabs.length; i > insertPosition; i--) {
    		   $scope.model.tabs[i] = $scope.model.tabs[i - 1]; 
    	   }
    	   if (!tabText) {
    		   if (nameArg) tabText = nameArg;
    		   else tabText = form;
    	   }
    	   $scope.model.tabs[insertPosition] = {
    			   name: nameArg,
    			   containsFormId: form,
    			   text: tabText,
    			   relationName: relation,
    			   active: false,
    			   disabled: false,
    			   foreground: fg };
    	   $scope.model.tabIndex = $scope.getTabIndex($scope.getSelectedTab());
    	   return true;
       }
       
       $scope.api.removeTabAt = function(index) {
    	   if(index > 0 && index <= $scope.model.tabs.length) {
        	   for(var i = index - 1; i < $scope.model.tabs.length - 1; i++) {
        		   $scope.model.tabs[i] = $scope.model.tabs[i + 1];
        	   }
        	   $scope.model.tabs.length = $scope.model.tabs.length - 1;
        	   $scope.model.tabIndex = $scope.getTabIndex($scope.getSelectedTab());
        	   return true;
    	   }
    	   return false;
       }
       
       $scope.api.removeAllTabs = function() {
    	   if($scope.model.tabs.length > 0) {
    		   $scope.model.tabs.length = 0;
    		   $scope.model.tabIndex = $scope.getTabIndex($scope.getSelectedTab());
    		   return true;
    	   }
    	   return false;
       }
       
       $scope.api.getMaxTabIndex = function() {
    	   return $scope.model.tabs.length; 
       }
       
       $scope.api.getSelectedTabFormName = function() {
    	   var selectedTab = $scope.getSelectedTab(); 
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
    	   return 'TABPANEL';
       }
       
       $scope.api.getName = function() {
    	   return $scope.model.name;
       }
      },
      template: "<div style='height:100%;width:100%;position:absolute;' svy-border='model.borderType'svy-font='model.fontType'><div ng-include='getTemplateUrl()'></div></div>",
      replace: true
    };
  })

  
  
  
  
