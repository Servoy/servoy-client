servoyModule.directive('svyTabpanel', function($utils) {  
    return {
      restrict: 'E',
      transclude: true,
      scope: {
        model: "=svyModel",
        svyApply: "=",
        svyServoyapi: "="
      },
      controller: function($scope, $element, $attrs) {
       var selectedTab;
       $scope.style = {}
       $utils.watchProperty($scope,'model.background',$scope.style,'backgroundColor')
       $utils.watchProperty($scope,'model.foreground',$scope.style,'color')
       
       $scope.getForm = function(tab) {
       	if (tab == selectedTab) {
       		return tab.containsFormId;
       	}
       	return "";
       }
       
       function setFormVisible(tab) {
       	var promise = $scope.svyServoyapi.setFormVisibility(tab.containsFormId,true, tab.relationName);
       	promise.then(function(ok) {
       		if (ok){
       			selectedTab = tab;
       		} else {
       			// will this ever happen?
       		}
       	});
       }
       
       $scope.select = function(tab) {
        if (selectedTab) {
        	var promise =  $scope.svyServoyapi.setFormVisibility(selectedTab.containsFormId,false);
        	promise.then(function(ok) {
        		if (ok) {
        			setFormVisible(tab);
        		}
        		else {
        			tab.active = false;
        			selecteTab.active = true;
        		}
        	})
        }
        else {
        	setFormVisible(tab);
        }
       }
      },
      templateUrl: 'servoydefault/tabpanel/tabpanel.html',
      replace: true
    };
  })

  
  
  
  
