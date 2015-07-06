angular.module('servoyextraDbtreeview',['servoy', 'foundset_manager']).directive('servoyextraDbtreeview', function($window, $foundsetManager) {  
    return {
      restrict: 'E',
      scope: {
    	  model: "=svyModel",
    	  api: "=svyApi"
      },
      link: function($scope, $element, $attrs) {

    	$scope.expandedNodes = [];
    	var theTree;

    	function initTree() {
      		theTree = $element.find(".dbtreeview").fancytree(
     	 	{
 				source: $scope.treeJSON,
 				selectMode: 2,
 				checkbox: true,
				select: function(event, data) {
					var v = data.node.selected
					if("number" == data.node.data.checkboxvaluedataprovidertype) {
						v = v ? 1 : 0;
					} else if ("string" == data.node.data.checkboxvaluedataprovidertype) {
						v = v ? 'true' : 'false'
					}
					$foundsetManager.updateFoundSetRow(
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
						$window.executeInlineScript(
								data.node.data.callbackinfo.formname,
								data.node.data.callbackinfo.script,
								[data.node.data.callbackinfoParamValue]);
					}					
				}
 			});
      		theTree = theTree.fancytree("getTree");
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
    		if(binding.callbackinfo) {
    			dataproviders[binding.callbackinfo.param] = binding.callbackinfo.param;
    		}
    		if(binding.methodToCallOnCheckBoxChange) {
    			dataproviders[binding.methodToCallOnCheckBoxChange.param] = binding.methodToCallOnCheckBoxChange.param;
    		}    		
    		
    		return dataproviders;
    	}

    	
    	function getRelatedFoundSetCallback(item) {
    		return function(rfoundsetinfo) {
				$foundsetManager.getFoundSet(
						rfoundsetinfo.foundsethash,
						getDataproviders(rfoundsetinfo.foundsetdatasource, rfoundsetinfo.foundsetpk)).then(
								function(rfoundset) {
									item.children = getChildren(rfoundset, rfoundsetinfo.foundsethash, rfoundsetinfo.foundsetpk, getBinding(rfoundsetinfo.foundsetdatasource));
									if(item.children.length > 0) {
										item.folder = "true";
									}
									theTree.reload($scope.treeJSON);
								});
			}
    	}
    	
    	
    	function getChildren(foundset, foundsethash, foundsetpk, binding) {
    		var returnChildren = new Array();
    		for(var i = 0; i < foundset.viewPort.rows.length; i++) {
    			var item = {};
    			item.key =  foundsethash + '_' + foundset.viewPort.rows[i][foundsetpk]; 
    			item.title = foundset.viewPort.rows[i][binding.textdataprovider];
    			if(binding.tooltiptextdataprovider) item.tooltip = foundset.viewPort.rows[i][binding.tooltiptextdataprovider];
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
 	
    			if(binding.callbackinfo || binding.methodToCallOnCheckBoxChange)
    			{
    				if(binding.callbackinfo) {
    					item.data.callbackinfo = binding.callbackinfo.f;
    					item.data.callbackinfoParamValue = foundset.viewPort.rows[i][binding.callbackinfo.param];
    				}
    				if(binding.methodToCallOnCheckBoxChange) {
    					item.data.methodToCallOnCheckBoxChange = binding.methodToCallOnCheckBoxChange.f;
    					item.data.methodToCallOnCheckBoxChangeParamValue = foundset.viewPort.rows[i][binding.methodToCallOnCheckBoxChange.param];
    				}    				
    			}

    			returnChildren.push(item);
    			if(binding.nrelationname) {
					$foundsetManager.getRelatedFoundSetHash(
							foundsethash,
							foundset.viewPort.rows[i]._svyRowId,
							binding.nrelationname).then(getRelatedFoundSetCallback(item));
    			}
    		}
    		
    		return returnChildren;
    	}
    	 
    	function findNode(node, pkarray, level) {
    		if(pkarray.length > 0) {
    			var nodeChildren = node.getChildren();
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
    		return null;
    	}
    	
  		$scope.$watch('model.roots', function(newValue) {
  			$scope.api.refresh();
		})
  		
      	$scope.api.refresh = function() {
			if($scope.model.roots && $scope.model.roots.length > 0) {
				
				$scope.expandedNodes.length = 0;
				if(theTree) {	  			
		  			theTree.getRootNode().visit(function(node){
		  				if(node.isExpanded()) {
		  					$scope.expandedNodes.push(node.key);
		  				}
			        });
	      		}
				
				$foundsetManager.getFoundSet($scope.model.roots[0].foundsethash, getDataproviders($scope.model.roots[0].foundsetdatasource, $scope.model.roots[0].foundsetpk)).then(
						function(foundset) {			
							$scope.treeJSON = getChildren(foundset, $scope.model.roots[0].foundsethash, $scope.model.roots[0].foundsetpk, getBinding($scope.model.roots[0].foundsetdatasource), 0);
							if(theTree) {
								theTree.reload($scope.treeJSON);
							}
							else {
								initTree();
							}
				});
			}
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
      		if(theTree) {	  			
	  			var node = findNode(theTree.getRootNode(), pk, 0);
	  			if(node) {
	  				node.makeVisible();
	  				node.setExpanded(state);
	  			}
      		}      		
      	}      	
      },
      templateUrl: 'servoyextra/dbtreeview/dbtreeview.html'
    };
  })
