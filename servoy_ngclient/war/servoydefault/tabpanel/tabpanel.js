angular.module('svyTabpanel',['servoy']).directive('svyTabpanel', function() {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        svyApply: "=",
        svyServoyapi: "=",
        handlers: "=svyHandlers"
      },
      controller: function($scope, $element, $attrs) {
       var selectedTab;
       $scope.bgstyle = {}
       
        $scope.$watch("model.tabIndex", function(newValue) {
        	 if($scope.model.tabIndex == undefined) $scope.model.tabIndex = 0; // default it is 0 
        	 for(var i=0;i<$scope.model.tabs.length;i++) {
        	 	if (i == $scope.model.tabIndex) $scope.model.tabs[i].active = true;
        	 	else $scope.model.tabs[i].active = false;
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
    	   return selectedTab?selectedTab.containsFormId:"";
       }
       
       $scope.getForm = function(tab) {
       	if (tab == selectedTab) {
       		return tab.containsFormId;
       	}
       	return "";
       }
       
       function setFormVisible(tab,event) {
       	var promise = $scope.svyServoyapi.setFormVisibility(tab.containsFormId,true, tab.relationName);
       	promise.then(function(ok) {
       		if (ok){
       			if(selectedTab != tab)
       			{
	       			for(var i=0;i<$scope.model.tabs.length;i++) {
	       				if ($scope.model.tabs[i] == selectedTab) {
	       					$scope.handlers.onChangeMethodID(i + 1,event);
	       				}
	       			}
       			}           			
       			selectedTab = tab;   			
       		} else {
       			// will this ever happen?
       		}
       	});
       }
       
       $scope.select = function(tab) {
    	if (tab == selectedTab) return;
        if (selectedTab) {
        	var selectEvent = event;
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
        	setFormVisible(tab);
        }
       }
      },
      template: "<div style='min-height:100%' svy-border='model.borderType'svy-font='model.fontType'><div ng-include='getTemplateUrl()'></div></div>",
      replace: true
    };
  })

  
  
  
  
