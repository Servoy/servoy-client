angular.module('servoydefaultTabpanel',['servoy']).directive('servoydefaultTabpanel', function($window, $log) {  
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
			$scope.bgstyle = {}
			$scope.waitingForServerVisibility = {}


			function refresh() {
				if($scope.model.tabIndex == undefined) $scope.model.tabIndex = 1; // default it is 1
				var realTabIndex = $scope.model.tabIndex - 1;
				if ($scope.model.tabs)
					for(var i=0;i<$scope.model.tabs.length;i++) {
						if (i == realTabIndex)
						{	
							$scope.model.tabs[i].active = true;
						}
						else $scope.model.tabs[i].active = false;
						$scope.model.tabs[i].disabled = false;
					}
			}

			$scope.$watch("model.tabIndex", function(newValue) {
				$log.debug("svy * model.tabIndex = " + $scope.model.tabIndex + " -- " + new Date().getTime());
				refresh();
			});

			$scope.$watch("model.tabs", function(newValue) {
				$log.debug("svy * model.tabs reference updated; length = " + ($scope.model.tabs ? $scope.model.tabs.length : undefined) + " -- " + new Date().getTime());
				refresh();
			});        

			$scope.$watch("model.readOnly", function(newValue) {
				var selectedTab = $scope.getSelectedTab()
				if (selectedTab && selectedTab.containsFormId)
				{
					$scope.svyServoyapi.setFormReadOnly(selectedTab.containsFormId,newValue);
				}
			});
			$scope.$watch("model.enabled", function(newValue) {
				var selectedTab = $scope.getSelectedTab()
				if (selectedTab && selectedTab.containsFormId)
				{
					$scope.svyServoyapi.setFormEnabled(selectedTab.containsFormId,newValue);
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
				return $scope.model.selectedTab?$scope.svyServoyapi.getFormUrl($scope.model.selectedTab.containsFormId):"";
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
				if ($scope.model.selectedTab && tab.containsFormId == $scope.model.selectedTab.containsFormId) {
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
				if($scope.model.selectedTab && $scope.model.selectedTab != tab && $scope.handlers.onChangeMethodID)
				{
					$scope.handlers.onChangeMethodID($scope.getTabIndex($scope.model.selectedTab),event instanceof MouseEvent ? event : null);
				}   			
				$log.debug("svy * selectedTab = '" + tab.containsFormId + "' -- " + new Date().getTime());
				$scope.model.selectedTab = tab;
				$scope.model.tabIndex = $scope.getTabIndex($scope.model.selectedTab);
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
				$log.debug("svy * Will select tab '" + (tab ? tab.containsFormId : undefined) + "'. Previously selected: '" + ($scope.model.selectedTab ? $scope.model.selectedTab.containsFormId : undefined) + "'. Same: " + (tab == $scope.model.selectedTab));
				if ((tab != undefined && $scope.model.selectedTab != undefined && tab.containsFormId == $scope.model.selectedTab.containsFormId) || (tab == $scope.model.selectedTab)) return;
				var selectEvent = $window.event ? $window.event : null;
				if ($scope.model.selectedTab) {
					if ($scope.model.selectedTab.containsFormId && !$scope.waitingForServerVisibility[$scope.model.selectedTab.containsFormId])
					{
						var formInWait = $scope.model.selectedTab.containsFormId;
						$scope.waitingForServerVisibility[formInWait] = true;
						var promise =  $scope.svyServoyapi.hideForm($scope.model.selectedTab.containsFormId);
						$log.debug("svy * Will hide previously selected form (tab): " + $scope.model.selectedTab.containsFormId);
						promise.then(function(ok) {
							$log.debug("svy * Previously selected form (tab) hide completed with '" + ok + "': " + $scope.model.selectedTab.containsFormId);
							delete $scope.waitingForServerVisibility[formInWait];
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

			// the api defined in the spec file
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
		template: "<div style='height:100%;width:100%;position:absolute;' svy-border='model.borderType'svy-font='model.fontType'><div ng-include='getTemplateUrl()'></div></div>"
	};
})





