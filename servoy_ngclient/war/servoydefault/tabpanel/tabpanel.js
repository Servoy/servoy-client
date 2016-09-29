angular.module('servoydefaultTabpanel',['servoy']).directive('servoydefaultTabpanel', function($window, $log, $apifunctions,$timeout,$anchorConstants) {  
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
				$scope.$watch("model.size", function (newVal) {
					$scope.viewportStyle = {height: ($element.children(0).height() - ($scope.model.tabs.length - 1) * groupHeaderHeight - activeGroupHeaderHeight - scrollHeight) + "px"};
				})
				if ((($scope.model.anchors & $anchorConstants.NORTH != 0) && ($scope.model.anchors & $anchorConstants.SOUTH != 0)) || (($scope.model.anchors & $anchorConstants.EAST != 0) && ($scope.model.anchors & $anchorConstants.WEST != 0)))
				{
					// window resize doesn't update the model size
					$window.addEventListener('resize',function() { 
						$scope.viewportStyle = {height: ($element.children(0).height() - ($scope.model.tabs.length - 1) * groupHeaderHeight - activeGroupHeaderHeight - scrollHeight) + "px"};
					});
				}
			}
			$scope.bgstyle = {}
			$scope.waitingForServerVisibility = {}

			if ($scope.model.selectedTab) {
			     // if the selected tab is already set then this is a reload of the form and we need to call formWillShow
				delete $scope.model.selectedTab;
			}
			
			function refresh() {
				var i = 0;
				var realTabIndex = 1;
				if($scope.model.tabIndex == undefined) $scope.model.tabIndex = 1; // default it is 1
				
				if (typeof $scope.model.tabIndex === 'number')
					realTabIndex = $scope.model.tabIndex - 1;
				else if (!isNaN(parseInt($scope.model.tabIndex)) && parseInt($scope.model.tabIndex) > 0)
				{
					realTabIndex = parseInt($scope.model.tabIndex) - 1;
				}	
				else 
					for(i=0;i<$scope.model.tabs.length;i++) {
						if ($scope.model.tabs[i].name === $scope.model.tabIndex)
						{	
							realTabIndex = i;
							break;
						}
					}
			    
				if ($scope.model.tabs){
					var selectedTabNotFound = true;
					
					for(i=0; i<$scope.model.tabs.length; i++) {
						
						if (i === realTabIndex)
						{	
							$scope.model.tabs[i].active = true;
							 
						}
						else 
							$scope.model.tabs[i].active = false;
						
						if ($scope.model.tabs[i] === $scope.model.selectedTab)
							selectedTabNotFound = false;
						
						$scope.model.tabs[i].disabled = false;
					}
					
					if (selectedTabNotFound)
						delete $scope.model.selectedTab;
				}
			}

			$scope.$watch("model.tabIndex", function(newValue) {
				if ($log.debugEnabled) $log.debug("svy * model.tabIndex = " + $scope.model.tabIndex + " -- " + new Date().getTime());
				refresh();
			});

			$scope.$watch("model.tabs", function(newValue) {
				if ($log.debugEnabled) $log.debug("svy * model.tabs reference updated; length = " + ($scope.model.tabs ? $scope.model.tabs.length : undefined) + " -- " + new Date().getTime());
				refresh();
			});        

			$scope.$watch("model.visible", function(newValue,oldValue) {
	    	  		if ($scope.model.selectedTab && newValue !== oldValue && $scope.model.selectedTab.containsFormId)
	    	  		{
	    	  			if (newValue)
	    	  			{
	    	  				$scope.svyServoyapi.formWillShow($scope.model.selectedTab.containsFormId,$scope.model.selectedTab.relationName);
	    	  			}
	    	  			else
	    	  			{
	    	  				$scope.svyServoyapi.hideForm($scope.model.selectedTab.containsFormId);
	    	  			}	
	  			}	
	  		  });
			 
			$scope.getTemplateUrl = function() {
				if ($scope.model.tabOrientation == -1 || ($scope.model.tabOrientation == 0 && $scope.model.tabs.length == 1)) return "servoydefault/tabpanel/tablesspanel.html";
				else if($scope.model.tabOrientation == -4) return "servoydefault/tabpanel/accordionpanel.html"
				else return "servoydefault/tabpanel/tabpanel.html";
			}
			$scope.getActiveTabUrl = function() {
				for(var i=0;i<$scope.model.tabs.length;i++) {
					if ($scope.model.tabs[i].active) {
						if ($scope.model.selectedTab != $scope.model.tabs[i])
						{
							$scope.select($scope.model.tabs[i]);
						} 
						break;
					}
				}
				if ($scope.model.selectedTab && !$scope.waitingForServerVisibility[$scope.model.selectedTab.containsFormId])
					return $scope.svyServoyapi.getFormUrl($scope.model.selectedTab.containsFormId);
				else
					return "";
			}

			$scope.getSelectedTab = function() {
				return $scope.model.selectedTab;
			}

			$scope.getTabAt = function(index) {
				if(index > 0 && index <= $scope.model.tabs.length) {
					return $scope.model.tabs[index - 1];
				}
			}

			$scope.getForm = function(tab) {
				if ($scope.model.selectedTab && (tab.containsFormId == $scope.model.selectedTab.containsFormId) && (tab.relationName == $scope.model.selectedTab.relationName)) {
					return $scope.svyServoyapi.getFormUrl(tab.containsFormId);
				}
				return "";
			}

			$scope.getActiveForm = function(tab) {
				if (tab && tab.active == true) {
					return $scope.svyServoyapi.getFormUrl(tab.containsFormId);
				}
				return "";
			}

			function setFormVisible(tab,event) {
				if (tab.containsFormId) $scope.svyServoyapi.formWillShow(tab.containsFormId, tab.relationName);
				if ($log.debugEnabled) $log.debug("svy * selectedTab = '" + tab.containsFormId + "' -- " + new Date().getTime());
				var oldSelected = $scope.model.selectedTab;
				$scope.model.selectedTab = tab;
				$scope.model.tabIndex = $scope.getTabIndex($scope.model.selectedTab);
				if(oldSelected && oldSelected != tab && $scope.handlers.onChangeMethodID)
				{
					$timeout(function() {
						$scope.handlers.onChangeMethodID($scope.getTabIndex(oldSelected),event !=null?event : $.Event("change"));
					},0,false);
				} 
			}

			$scope.getTabIndex = function(tab) {
				if(tab) {
					for(var i=0;i<$scope.model.tabs.length;i++) {
						if (($scope.model.tabs[i].containsFormId == tab.containsFormId) && ($scope.model.tabs[i].relationName == tab.relationName)) {
							return i + 1;
						}
					}
				}
				return -1;
			}

			$scope.select = function(tab) {
				if ($log.debugEnabled) $log.debug("svy * Will select tab '" + (tab ? tab.containsFormId : undefined) + "'. Previously selected: '" + ($scope.model.selectedTab ? $scope.model.selectedTab.containsFormId : undefined) + "'. Same: " + (tab == $scope.model.selectedTab));
				if ((tab != undefined && $scope.model.selectedTab != undefined && tab.containsFormId == $scope.model.selectedTab.containsFormId && tab.relationName == $scope.model.selectedTab.relationName) || (tab == $scope.model.selectedTab)) return;
				var selectEvent = $window.event ? $window.event : null;
				if ($scope.model.selectedTab) {
					if ($scope.model.selectedTab.containsFormId && !$scope.waitingForServerVisibility[$scope.model.selectedTab.containsFormId])
					{
						var formInWait = $scope.model.selectedTab.containsFormId;
						$scope.waitingForServerVisibility[formInWait] = true;
						var promise =  $scope.svyServoyapi.hideForm($scope.model.selectedTab.containsFormId,null,null,tab.containsFormId, tab.relationName);
						if ($log.debugEnabled) $log.debug("svy * Will hide previously selected form (tab): " + $scope.model.selectedTab.containsFormId);
						promise.then(function(ok) {
							if ($log.debugEnabled) $log.debug("svy * Previously selected form (tab) hide completed with '" + ok + "': " + $scope.model.selectedTab.containsFormId);
							delete $scope.waitingForServerVisibility[formInWait];
							if (!tab.active)
							{
								// visibility changed again, just ignore this
								if ($log.debugEnabled) $log.debug("svy * Tab '" + tab.containsFormId + "': no longer active, ignore making it visible");
								return;
							}
							if (ok) {
								setFormVisible(tab,selectEvent);
							}
							else {
								tab.active = false;
								$scope.model.selectedTab.active = true;
							}
						})
					}
				}
				else {
					setFormVisible(tab, selectEvent);
				}
			}

			$scope.getTabsHeight = function() {
    		  return {top:$element.find(".nav-tabs").height()+"px"};
    	 	}

			// the api defined in the spec file
			/**
			 * Adds a relationless or related form as a tab in a specified tabpanel.
			 * @example %%prefix%%%%elementName%%.addTab(forms.orders,'ordersTab','Orders',null,null,'#000000','#BBCCEE');
			 * @param form/formname the specified form/form name you wish to add as a tab
			 * @param name optional the specified name for the tab or NULL (default is null)
			 * @param tabText optional the specified text for the tab (default is null)
			 * @param tooltip optional a specified tooltip for the tab (default is null)
			 * @param iconURL optional a specified icon image or icon URL for the tab (default is null)
			 * @param fg optional the HTML RGB Hexadecimal foreground color for the tab (default is null)
			 * @param bg optional the HTML RGB Hexadecimal background color for the tab (default is null)
			 * @param relatedfoundset/relationname optional the specified name of the related foundset (default is null)
			 * @param index optional the specified index of a tab, default is -1, will add tab to the end, this index is 0 based
			 * 
			 * @return {Boolean} a value indicating if tab was successfully added
			 */
			$scope.api.addTab = function(form, nameArg, tabText, tooltip, iconURL, fg, bg, relation, index) {
				// addTab from tabpanel_server.js will usually execute instead of this one (almost in all cases) - some client side only tests use this method
				// the same is true for other API methods as well
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

			/**
		   	 * Removes a specified tab in a tabpanel; can be based on a relation or relationless.
		   	 * @example %%prefix%%%%elementName%%.removeTabAt(3)
		   	 * @param index The index of the tab to remove.
		   	 * @return {Boolean} a value indicating if tab was successfully removed
		   	 */
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

			/**
		   	 * Removes all tabs in the tabpanel.
		   	 * @example %%prefix%%%%elementName%%.removeTabAt(3)
		   	 * @return {Boolean} a value indicating if tabs were successfully removed
		   	 */
			$scope.api.removeAllTabs = function() {
				if($scope.model.tabs.length > 0) {
					$scope.model.tabs.length = 0;
					$scope.model.tabIndex = $scope.getTabIndex($scope.getSelectedTab());
					return true;
				}
				return false;
			}
			
			/**
	      	  * Returns the maximum tab index for a specified tabpanel.
	      	  * @example var max = %%prefix%%%%elementName%%.getMaxTabIndex();
	      	  * @return {number} maximum tab index, 1 in case of the splitpane
	      	  */
			$scope.api.getMaxTabIndex = function() {
				return $scope.model.tabs.length; 
			}

			 /**
	      	  * Returns form name of the selected tab in the tabpanel.
	      	  * @example var formName = %%prefix%%%%elementName%%.getSelectedTabFormName();
	      	  * @return {String} the name of the form
	      	  */
			$scope.api.getSelectedTabFormName = function() {
				var selectedTab = $scope.getSelectedTab(); 
				return selectedTab ? selectedTab.containsFormId : null;
			}

			$scope.api.getTabBGColorAt = function(index) {
				return null;
			}

			 /**
	      	  * Returns the foreground color for a specified tab of a tabpanel. 
	      	  * @example var color = %%prefix%%%%elementName%%.getTabFGColorAt(3); 
	      	  * @param i the number of the specified tab
	      	  * @return {String} color as hexadecimal RGB string
	      	  */
			$scope.api.getTabFGColorAt = function(index) {
				var tab = $scope.getTabAt(index);
				return tab ? tab.foreground  : '';
			}

			/**
	      	  * Returns the form name for a specified tab of the tabpanel.
	      	  * @example var formName = %%prefix%%%%elementName%%.getTabFormNameAt(3);
	      	  * @param i index of the tab
	      	  * @return {String} the name of the form
	      	  */
			$scope.api.getTabFormNameAt = function(index) {
				var tab = $scope.getTabAt(index);
				return tab ? tab.containsFormId  : '';
			}

			/**
	      	  * Returns the name - the "name" design time property value - for a specified tab of the tabpanel. 
	      	  * @example var tabName = %%prefix%%%%elementName%%.getTabNameAt(3);
	      	  * @param i The number of the specified tab.
	      	  * @return {String} The tab name
	      	  */
			$scope.api.getTabNameAt = function(index) {
				var tab = $scope.getTabAt(index);
				return tab ? tab.name  : '';
			}

			 /**
	     	  * Returns the relation name for a specified tab of the tabpanel.
	     	  * @example var relName = %%prefix%%%%elementName%%.getTabRelationNameAt(3);
	     	  * @param i index of the tab
	     	  * @return {String} relation name
	     	  */
			$scope.api.getTabRelationNameAt = function(index) {
				var tab = $scope.getTabAt(index);
				return tab ? tab.relationName  : '';
			}

			 /**
	      	  * Returns the text for a specified tab of a tabpanel. 
	      	  * @example var tabText = %%prefix%%%%elementName%%.getTabTextAt(3);
	      	  * @param i The number of the specified tab.
	      	  * @return {String} The tab text.
	      	  */
			$scope.api.getTabTextAt = function(index) {
				var tab = $scope.getTabAt(index);
				return tab ? tab.text  : '';
			}

			/**
	      	  * Returns the enabled status of a specified tab in a tabpanel.
	      	  * @example var status = %%prefix%%%%elementName%%.isTabEnabled(3); 
	      	  * @param i the number of the specified tab
	      	  * @return {Boolean} True if tab is enabled, false otherwise.
	      	  */
			$scope.api.isTabEnabled = function(index) {
				return $scope.api.isTabEnabledAt(index);
			}

			/**
	      	  * Returns the enabled status of a specified tab in a tabpanel.
	      	  * @example var status = %%prefix%%%%elementName%%.isTabEnabled(3); 
	      	  * @param i the number of the specified tab
	      	  * @return {Boolean} True if tab is enabled, false otherwise.
	      	  */
			$scope.api.isTabEnabledAt = function(index) {
				var tab = $scope.getTabAt(index);
				return tab ? (tab.disabled == undefined ? true : !tab.disabled) : true;
			}

			
			$scope.api.setTabBGColorAt = function(index, bgcolor) {
			}

			/**
	      	  * Sets the status of a specified tab in a tabpanel.
	      	  * @example %%prefix%%%%elementName%%.setTabEnabled(1,true);
	       	  * @param i the number of the specified tab.
	      	  * @param b true if enabled; or false if disabled.
	      	  */
			$scope.api.setTabEnabled = function(index, enabled) {
				$scope.api.setTabEnabledAt(index, enabled);
			}

			/**
	      	  * Sets the status of a specified tab in a tabpanel.
	      	  * @example %%prefix%%%%elementName%%.setTabEnabledAt(1,true);
	       	  * @param i the number of the specified tab.
	      	  * @param b true if enabled; or false if disabled.
	      	  */
			$scope.api.setTabEnabledAt = function(index, enabled) {
				var tab = $scope.getTabAt(index);
				if(tab) {
					tab.disabled = !enabled;
				}
			}

			/**
	           * Sets the foreground color for a specified tab in a splitpane.
	      	   * @example %%prefix%%%%elementName%%.setTabFGColorAt(3,'#000000');
	      	   * @param i the number of the specified tab
	      	   * @param s the hexadecimal RGB color value to be set.
	      	  */
			$scope.api.setTabFGColorAt = function(index, fgcolor) {
				var tab = $scope.getTabAt(index);
				if(tab) {
					tab.foreground = fgcolor;
				}    	   
			}

			/**
			 * Sets the text for a specified tab in a tabpanel.
			 * @example %%prefix%%%%elementName%%.setTabTextAt(3,'newTitle');
			 * @param index the number of the specified tab
			 * @param text the text to be set for the specified tab
			 */
			$scope.api.setTabTextAt = function(index, text) {
				var tab = $scope.getTabAt(index);
				if(tab) {
					tab.text = text;
				}    	    	   
			}

			$scope.api.getHeight = function() {
				return $scope.model.anchors ? $apifunctions.getHeight($element[0])() : $scope.model.size.height;
			}

			$scope.api.getLocationX = function() {
				return $scope.model.anchors ? $apifunctions.getX($element[0])() : $scope.model.location.x;
			}

			$scope.api.getLocationY = function() {
				return $scope.model.anchors ? $apifunctions.getY($element[0])() : $scope.model.location.y;
			}

			$scope.api.getWidth = function() {
				return $scope.model.anchors ? $apifunctions.getWidth($element[0])() : $scope.model.size.width;
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
		template: "<div style='height:100%;width:100%;position:absolute;' svy-border='model.borderType'svy-font='model.fontType'><div ng-include='getTemplateUrl()'></div></div>"
	};
})
