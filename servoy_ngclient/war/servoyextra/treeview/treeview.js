angular.module('svytreeview',['servoy']).directive('svytreeview', function() {  
    return {
      restrict: 'E',
      scope: {
    	  model: "=svyModel",
    	  api: "=svyApi",
    	  handlers: "=svyHandlers"
      },
      link: function($scope, $element, $attrs) {
    	
    	$scope.model.fancyTreeJSON = new Array();
    	var theTree;
    	
      	function initTree() {
      		theTree = $element.find("div").fancytree(
     	 	{
 				source: $scope.model.fancyTreeJSON,
 				selectMode: 1,
// 				extensions: ["wide"],
 				activate: function(event, data) {
					if(!data.node.isSelected()) data.node.setSelected();
					if($scope.handlers.onNodeClicked) $scope.handlers.onNodeClicked(data.node.key);
				},
				select: function(event, data) {
					if($scope.handlers.onNodeSelected) $scope.handlers.onNodeSelected(data.node.key);
				},
				expand: function(event, data) {
					if($scope.handlers.onNodeExpanded) $scope.handlers.onNodeExpanded(data.node.key);
				},
				collapse: function(event, data) {
					if($scope.handlers.onNodeCollapsed) $scope.handlers.onNodeCollapsed(data.node.key);
				}
// 				wide: {
// 					iconWidth: "1em",     // Adjust this if @fancy-icon-width != "16px"
// 					iconSpacing: "0.5em", // Adjust this if @fancy-icon-spacing != "3px"
// 					levelOfs: "1.5em"     // Adjust this if ul padding != "16px"
// 				},
 			});
      		theTree = theTree.fancytree("getTree");
     	}
      	
     	function getTree() {
     		if(!theTree) {
     			initTree();
     		}
     		return theTree;
     	}

  		function findParent(parentId, children) {
  			if(children != null) {
  				for(var i = 0; i < children.length; i++) {
  					var p = (parentId == children[i].key) ? children[i] : findParent(parentId, children[i].children ? children[i].children : null); 
  					if(p != null) return p;
  				}
  			}
  			return null;
  		};
      	
		/**
		 * Sets the tree data
		 * @param jsDataSet the JSDataSet used for the tree model
		 * @example
		 * 	var treeviewDataSet = databaseManager.createEmptyDataSet( 0,  ['id', 'pid', 'treeColumn', 'icon']);
		 * 
		 *	treeviewDataSet.addRow([1,		null,	'Main group',	'media:///group.png']);
		 *	treeviewDataSet.addRow([2,		null,	'Second group',	'media:///group.png']);
		 *	treeviewDataSet.addRow([3,		2,		'Subgroup',		'media:///group.png']);
		 *	treeviewDataSet.addRow([4,		3,		'Mark',			'media:///user.png']);
		 *	treeviewDataSet.addRow([5,		3,		'George',		'media:///user.png']);
		 *
		 *	%%prefix%%%%elementName%%.setDataSet(treeviewDataSet);
		 */  		
      	$scope.api.setDataSet = function(jsDataSet) {
      		$scope.model.fancyTreeJSON.length = 0;
      		var idIdx = jsDataSet[0].indexOf("id");
      		var pidIdx = jsDataSet[0].indexOf("pid")
      		var iconIdx = jsDataSet[0].indexOf("icon");
      		var treeColumnIdx = jsDataSet[0].indexOf("treeColumn");
      		for(var i = 1; i < jsDataSet.length; i++) {
      			var n = {key: jsDataSet[i][idIdx], title: jsDataSet[i][treeColumnIdx]};
      			var icon = jsDataSet[i][iconIdx];
      			if(icon) n.icon = icon;
      			var parentChildren = $scope.model.fancyTreeJSON;
      			var p = findParent(jsDataSet[i][pidIdx], $scope.model.fancyTreeJSON);
      			if(p != null) {
      				if(!p.children) {
      					p.children = new Array();
      				}
      				parentChildren = p.children;
      				p.folder = "true";
      			}
      			parentChildren.push(n);
      		}
      		initTree();
      	}
      	
      	$scope.api.refresh = function(restoreExpandedNodes) {
      		getTree().reload();
      	}
      	
      	$scope.api.expandNode = function(nodeId) {
  			var node = getTree().getNodeByKey(nodeId.toString());
  			if(node) {
  				node.makeVisible();
  				node.setExpanded(true);
  			}
      	}

      	$scope.api.isNodeExpanded = function(nodeId) {
  			var node = getTree().getNodeByKey(nodeId.toString());
  			if(node) {
  				return node.isExpanded();
  			}
      		return false;
      	}

      	$scope.api.collapseNode = function(nodeId) {
  			var node = getTree().getNodeByKey(nodeId.toString());
  			if(node) {
  				node.setExpanded(false);
  			}
      	}

      	$scope.api.setSelectedNode = function(nodeId) {
  			var node = getTree().getNodeByKey(nodeId.toString());
  			if(node) {
  				node.setSelected()
  			}
      	}

      	$scope.api.getSeletedNode = function() {
  			var nodes = getTree().getSelectedNodes();
  			if(nodes && nodes.length && nodes.length > 0) {
  				return nodes[0].key;
  			}
      		return null;
      	}

      	$scope.api.getChildNodes = function(nodeId) {
      		var childNodesId = new Array();
  			var node = getTree().getNodeByKey(nodeId.toString());
  			if(node && node.children) {
  				for(var i = 0; i < node.children.length; i++) {
  					childNodesId.push(node.children[i].key);
  				}
  			}
      		return childNodesId;
      	}
      	
      	$scope.api.getParentNode = function(nodeId) {
  			var node = getTree().getNodeByKey(nodeId.toString());
  			if(node && node.parent) {
  				return node.parent.key;
  			}
      		return null;
      	}
      	
      	$scope.api.getNodeLevel = function(nodeId) {
  			var node = getTree().getNodeByKey(nodeId.toString());
  			if(node) {
  				return node.getLevel();
  			}
      		return -1;
      	}
      	
      	$scope.api.getRootNodes = function() {
      		var rootNodesId = new Array();
  			var node = getTree().getRootNode();
  			if(node && node.children) {
  				for(var i = 0; i < node.children.length; i++) {
  					rootNodesId.push(node.children[i].key);
  				}
  			}
      		return rootNodesId;
      	}
      	
      },
      templateUrl: 'servoyextra/treeview/treeview.html'
    };
  })
