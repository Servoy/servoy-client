angular.module('servoyextraDbtreeview', ['servoyApp','foundset_manager']).directive('servoyextraDbtreeview', ['$timeout', '$window', 'foundset_manager', '$applicationService', '$q', function($timeout, $window, foundset_manager, $applicationService, $q) {
    return {
      restrict: 'E',
      scope: {
    	  model: "=svyModel",
    	  api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {    	  
    	$scope.expandedNodes = [];
    	$scope.pendingChildrenRequests = 0;
    	$scope.pendingRefresh = false;
    	var theTree;
    	var clickTimeout;
    	var theTreeDefer = $q.defer();
  	
    	var foundsetChangeWatches = {};
  
    	var treeReloadTimeout;
    	function reloadTree() {
			if(treeReloadTimeout) {
				$timeout.cancel(treeReloadTimeout);
			}
			treeReloadTimeout = $timeout(function() {
				if(theTree) {
					theTree.reload($scope.treeJSON);
				}
			}, 200);
    	}
    	
    	function initTree() {
      		theTree = $element.find(".dbtreeview").fancytree(
     	 	{
 				source: $scope.treeJSON,
 				selectMode: 2,
 				scrollParent: $element.find(".dbtreeview"),
 				checkbox: true,
				select: function(event, data) {
					var v = data.node.selected
					if("number" == data.node.data.checkboxvaluedataprovidertype) {
						v = v ? 1 : 0;
					} else if ("string" == data.node.data.checkboxvaluedataprovidertype) {
						v = v ? 'true' : 'false'
					}
					foundset_manager.updateFoundSetRow(
							parseInt(data.node.key.substring(0, data.node.key.indexOf('_'))),
							data.node.data._svyRowId,
							data.node.data.checkboxvaluedataprovider,
							v);
					
					if(data.node.data && data.node.data.methodToCallOnCheckBoxChange) {
						$window.executeInlineScript(
								data.node.data.methodToCallOnCheckBoxChange.formname,
								data.node.data.methodToCallOnCheckBoxChange.script,
								[data.node.data.methodToCallOnCheckBoxChangeParamValue]);
					}
				},
				click: function(event, data) {
					if(data.node.data && data.node.data.callbackinfo) {
		    			if(clickTimeout) {
		    				$timeout.cancel(clickTimeout);
		    			}
		    			clickTimeout = $timeout(function() {
							$window.executeInlineScript(
									data.node.data.callbackinfo.formname,
									data.node.data.callbackinfo.script,
									[data.node.data.callbackinfoParamValue]);
		    			}, 200);
					}					
				},
				dblclick: function(event, data) {
					if(data.node.data && data.node.data.methodToCallOnDoubleClick) {
		    			if(clickTimeout) {
		    				$timeout.cancel(clickTimeout);
		    				clickTimeout = null;
		    			}						
						$window.executeInlineScript(
								data.node.data.methodToCallOnDoubleClick.formname,
								data.node.data.methodToCallOnDoubleClick.script,
								[data.node.data.methodToCallOnDoubleClickParamValue]);
					}
				},
				activate: function(even, data) {
					var selectionPath = [];
					var activeNode = data.node;
      				for(var i = 0; i < activeNode.getParentList().length; i++) {
      					var parentNode = activeNode.getParentList()[i];
        				var pkIdx = parentNode.key.indexOf('_');      					
      					selectionPath.push(parentNode.key.substring(pkIdx + 1));
      				}
      				var pkIdx = activeNode.key.indexOf('_');
      				selectionPath.push(activeNode.key.substring(pkIdx + 1));
      				$scope.model.selection = selectionPath;
				},
				init: function() {
		      		if(theTree) theTreeDefer.resolve(theTree);
				},
				lazyLoad: function(event, data){
					var nodeChildrenInfo = data.node.data.getChildren;				      
					data.result = getChildren(nodeChildrenInfo.foundset, nodeChildrenInfo.foundsethash, nodeChildrenInfo.foundsetpk, nodeChildrenInfo.binding, nodeChildrenInfo.level);
				}
 			});
      		theTree = theTree.fancytree("getTree");
      		theTreeDefer.resolve(theTree);
     	}
    	 
    	function getIconURL(iconPath) {
    		if(iconPath && iconPath.indexOf("media://") == 0) {
    			return "resources/fs/" + $applicationService.getSolutionName() + iconPath.substring(8);
    		}
    		return iconPath;
    	}
    	
      	function getBinding(datasource) {
    		for(var i = 0; i < $scope.model.bindings.length; i++) {
    			if(datasource == $scope.model.bindings[i].datasource) {
    				return $scope.model.bindings[i];
    			}
    		}
    		return null;
    	}        	  
    	  
    	function getDataproviders(datasource, foundsetpk) {
    		var dataproviders = {}
    		var binding = getBinding(datasource);
    		
    		dataproviders[foundsetpk] = foundsetpk;
    		
    		if(binding.textdataprovider) {
    			dataproviders[binding.textdataprovider] = binding.textdataprovider;
    		}
    		if(binding.textdataprovider) {
    			dataproviders[binding.textdataprovider] = binding.textdataprovider;
    		}
    		if(binding.hascheckboxdataprovider) {
    			dataproviders[binding.hascheckboxdataprovider] = binding.hascheckboxdataprovider;
    		}
    		if(binding.checkboxvaluedataprovider) {
    			dataproviders[binding.checkboxvaluedataprovider] = binding.checkboxvaluedataprovider;
    		}
    		if(binding.tooltiptextdataprovider) {
    			dataproviders[binding.tooltiptextdataprovider] = binding.tooltiptextdataprovider;
    		}
    		if(binding.imageurldataprovider) {
    			dataproviders[binding.imageurldataprovider] = binding.imageurldataprovider;
    		}
    		if(binding.childsortdataprovider) {
    			dataproviders[binding.childsortdataprovider] = binding.childsortdataprovider;
    		}    		    		
    		if(binding.callbackinfo) {
    			dataproviders[binding.callbackinfo.param] = binding.callbackinfo.param;
    		}
    		if(binding.methodToCallOnCheckBoxChange) {
    			dataproviders[binding.methodToCallOnCheckBoxChange.param] = binding.methodToCallOnCheckBoxChange.param;
    		}    		
    		if(binding.methodToCallOnDoubleClick) {
    			dataproviders[binding.methodToCallOnDoubleClick.param] = binding.methodToCallOnDoubleClick.param;
    		}    		    		

    		return dataproviders;
    	}

    	
    	function getRelatedFoundSetCallback(item, sort, level) {
    		return function(rfoundsetinfo) {
    			if(rfoundsetinfo) {
					foundset_manager.getFoundSet(
							rfoundsetinfo.foundsethash,
							getDataproviders(rfoundsetinfo.foundsetdatasource, rfoundsetinfo.foundsetpk), sort).then(
									function(rfoundset) {
										if(foundsetChangeWatches[rfoundsetinfo.foundsethash] != undefined) {
											foundsetChangeWatches[rfoundsetinfo.foundsethash]();
										}
										foundsetChangeWatches[rfoundsetinfo.foundsethash] = foundset_manager.addFoundSetChangeCallback(rfoundsetinfo.foundsethash, function() {
											if(jQuery.contains(document.documentElement, $element.get(0)) && ($scope.pendingChildrenRequests < 1)) {
												refresh();
											}
										});
										
										if(rfoundset && rfoundset.viewPort.rows.length > 0) {
											item.folder = true;
											item.lazy = true;
											
											item.data.getChildren = {
													foundset: rfoundset,
													foundsethash: rfoundsetinfo.foundsethash,
													foundsetpk: rfoundsetinfo.foundsetpk,
													binding: getBinding(rfoundsetinfo.foundsetdatasource),
													level: level
											}
										}
										$scope.pendingChildrenRequests = $scope.pendingChildrenRequests - 1;
									});
				}
				else {
					$scope.pendingChildrenRequests = $scope.pendingChildrenRequests - 1;
				}
			}
    	}
    	
    	
    	function getChildren(foundset, foundsethash, foundsetpk, binding, level) {
    		var returnChildren = new Array();
    		if(foundset) {
	    		for(var i = 0; i < foundset.viewPort.rows.length; i++) {
	    			var item = {};
	    			item.key =  foundsethash + '_' + foundset.viewPort.rows[i][foundsetpk]; 
	    			item.title = foundset.viewPort.rows[i][binding.textdataprovider];
	    			if(binding.tooltiptextdataprovider) item.tooltip = foundset.viewPort.rows[i][binding.tooltiptextdataprovider];
	    			if(binding.imageurldataprovider) item.icon = getIconURL(foundset.viewPort.rows[i][binding.imageurldataprovider]);
	    			item.hideCheckbox = binding.hascheckboxdataprovider == undefined || !foundset.viewPort.rows[i][binding.hascheckboxdataprovider];
	    			if(!item.hideCheckbox) {
	    				item.selected = Boolean(foundset.viewPort.rows[i][binding.checkboxvaluedataprovider])
	    			}
	    			
	    			if($scope.expandedNodes.indexOf(item.key) != -1) {
	    				item.expanded = true;
	    			}
	    			
	    			item.data = {}
	    			item.data._svyRowId = foundset.viewPort.rows[i]._svyRowId;
	    			
	    			if(binding.checkboxvaluedataprovider) {
	    				item.data.checkboxvaluedataprovider = binding.checkboxvaluedataprovider;
	    				item.data.checkboxvaluedataprovidertype = typeof foundset.viewPort.rows[i][binding.checkboxvaluedataprovider];
	    			}
	 	
	    			if(binding.callbackinfo || binding.methodToCallOnCheckBoxChange || binding.methodToCallOnDoubleClick)
	    			{
	    				if(binding.callbackinfo) {
	    					item.data.callbackinfo = binding.callbackinfo.f;
	    					item.data.callbackinfoParamValue = foundset.viewPort.rows[i][binding.callbackinfo.param];
	    				}
	    				if(binding.methodToCallOnCheckBoxChange) {
	    					item.data.methodToCallOnCheckBoxChange = binding.methodToCallOnCheckBoxChange.f;
	    					item.data.methodToCallOnCheckBoxChangeParamValue = foundset.viewPort.rows[i][binding.methodToCallOnCheckBoxChange.param];
	    				}
	    				if(binding.methodToCallOnDoubleClick) {
	    					item.data.methodToCallOnDoubleClick = binding.methodToCallOnDoubleClick.f;
	    					item.data.methodToCallOnDoubleClickParamValue = foundset.viewPort.rows[i][binding.methodToCallOnDoubleClick.param];
	    				}    				    				
	    			}
	    			
	    			returnChildren.push(item);
	    			
	    			if(binding.nrelationname) {
	    				$scope.pendingChildrenRequests = $scope.pendingChildrenRequests + 1; 
	    				var sort = binding.childsortdataprovider ? foundset.viewPort.rows[i][binding.childsortdataprovider]: null
						foundset_manager.getRelatedFoundSetHash(
								foundsethash,
								foundset.viewPort.rows[i]._svyRowId,
								binding.nrelationname).then(getRelatedFoundSetCallback(item, sort, level + 1));
	    			}
	    		} 
	    	}
    		return returnChildren;
    	}
    	 
    	function findNode(node, pkarray, level) {
    		if(pkarray && pkarray.length > 0) {
    			var nodeChildren = node.getChildren();
    			if(nodeChildren) {
	    			for(var i = 0; i < nodeChildren.length; i++) {
	    				var pkIdx = nodeChildren[i].key.indexOf('_');
	    				if(nodeChildren[i].key.substring(pkIdx + 1) == pkarray[level].toString()) {
	    					if(level + 1 < pkarray.length) {
	    						return findNode(nodeChildren[i], pkarray, level + 1);
	    					}
	    					else {
	    						return nodeChildren[i];
	    					}
	    				}
	    			}
	    		}
    		}
    		return null;
    	}
    	
    	function loadRoot(nr) {
				foundset_manager.getFoundSet($scope.model.roots[nr].foundsethash, getDataproviders($scope.model.roots[nr].foundsetdatasource, $scope.model.roots[nr].foundsetpk)).then(
						function(foundset) {
							if(foundsetChangeWatches[$scope.model.roots[nr].foundsethash] != undefined) {
								foundsetChangeWatches[$scope.model.roots[nr].foundsethash]();
							}							
							foundsetChangeWatches[$scope.model.roots[nr].foundsethash] = foundset_manager.addFoundSetChangeCallback($scope.model.roots[nr].foundsethash, function() {
								if(jQuery.contains(document.documentElement, $element.get(0)) && ($scope.pendingChildrenRequests < 1)) {
									refresh();
								}
							});
 
							$scope.treeJSON = $scope.treeJSON.concat(getChildren(foundset, $scope.model.roots[nr].foundsethash, $scope.model.roots[nr].foundsetpk, getBinding($scope.model.roots[nr].foundsetdatasource), 1));
							$scope.pendingChildrenRequests = $scope.pendingChildrenRequests - 1;
				});    	
    	}
  		
  		function selectNode(selection) {
			if(selection && selection.length) {
				theTreeDefer.promise.then(function(theTree) {
		  			var node = findNode(theTree.getRootNode(), selection, 0);
		  			if(node && !node.isActive()) {
		  				$timeout(function() {
		  							node.makeVisible({scrollIntoView: true});
		  							node.setActive(true);
								}, 200);
		  			}      			
	      		});
	      	}
  		}
  		
      	function refresh() {
			if($scope.pendingChildrenRequests < 1 && $scope.model.roots && $scope.model.roots.length > 0) {	
				for(var wKey in foundsetChangeWatches) {
					// foundset_manager.removeFoundSetFromCache(wKey);
					if(foundsetChangeWatches[wKey]) {
      					foundsetChangeWatches[wKey]();
      					delete foundsetChangeWatches[wKey];
      				}
      			}
      			foundset_manager.removeFoundSetsFromCache();
			
				$scope.pendingChildrenRequests = $scope.model.roots.length;
				$scope.expandedNodes.length = 0;
				if(theTree) {	  			
		  			theTree.getRootNode().visit(function(node){
		  				if(node.isExpanded()) {
		  					$scope.expandedNodes.push(node.key);
		  				}	
			        });
	      		}

				$scope.treeJSON = [];
				for(var i = 0; i < $scope.model.roots.length; i++) {
					$scope.pendingChildrenRequests = $scope.pendingChildrenRequests + 1; 
					loadRoot(i);
				}
				
				
				var pendingChildrenRequestsWatch = $scope.$watch('pendingChildrenRequests', function(nV) {
						if(nV == $scope.model.roots.length) {
							pendingChildrenRequestsWatch();
							if(theTree) {
								reloadTree();
							}
							else {
								initTree();
							}
							selectNode($scope.model.selection);

							$scope.pendingChildrenRequests = 0;
							if($scope.pendingRefresh) {
								$scope.pendingRefresh = false;
								refresh();
							}
						}
				})
			}
			else if($scope.pendingChildrenRequests > 0) {
				$scope.pendingRefresh = true;
			}
      	}

      	$scope.api.refresh = function() {
      		if($scope.pendingChildrenRequests > 0) {
      			$scope.pendingRefresh = true;
      			return;
      		}
      		theTreeDefer.reject();
      		theTreeDefer = $q.defer();
      		refresh();
      	}
      	
      	$scope.api.isNodeExpanded = function(pk) {
      		if(theTree) {	  			
	  			var node = findNode(theTree.getRootNode(), pk, 0);
	  			if(node) {
	  				return node.isExpanded();
	  			}
      		}
      		return false;
      	}

      	$scope.api.setExpandNode = function(pk, state) {
      		theTreeDefer.promise.then(function(theTree) {
	  			var node = findNode(theTree.getRootNode(), pk, 0);
	  			if(node) {
	  				node.makeVisible();
	  				node.setExpanded(state);
	  			}      			
      		});      		
      	}      	

      	function expandChildNodes(node, level, state) {
      		if(level >= 1) {
    			var nodeChildren = node.getChildren();
    			for(var i = 0; i < nodeChildren.length; i++) {
    				if(state) {
	    				nodeChildren[i].makeVisible();
	    				nodeChildren[i].setExpanded(state);
    				}
    				else if(level == 1) {
    					nodeChildren[i].setExpanded(state);
    				}
    				expandChildNodes(node, level - 1, state);
    			}
      		}
      	}
      	
      	$scope.api.getSelectionPath = function() {
      		return $scope.model.selection;
      	}
      	
  		$scope.$watch('model.roots', function(newValue) {
  			$scope.api.refresh();
		})
		
		$scope.$watch('model.selection', function(newValue) {
			selectNode(newValue);
		})
		
  		$scope.$watch('model.levelVisibility', function(newValue) {
  			if(newValue) {
				theTreeDefer.promise.then(function(theTree) {
					expandChildNodes(theTree.getRootNode(), newValue.level, newValue.state);	
	      		});
	      	}
		})		
		
      },
      templateUrl: 'servoyextra/dbtreeview/dbtreeview.html'
    };
  }]);
