{
	"name": "servoyextra-treeview",
	"displayName": "Treeview",
	"version": 1,
	"definition": "servoyextra/treeview/treeview.js",
	"serverscript": "servoyextra/treeview/treeview_server.js",
	"libraries": [{"name":"jquery-ui.min.js", "version":"1.11.4", "url":"servoyextra/treeview/js/jquery-ui.min.js", "mimetype":"text/javascript"}, {"name":"jquery.fancytree-all.min.js", "version":"2.9.0", "url":"servoyextra/treeview/js/jquery.fancytree-all.min.js", "mimetype":"text/javascript"},{"name":"ui.fancytree.css", "version":"2.9.0", "url":"servoyextra/treeview/css/skin-win8/ui.fancytree.css", "mimetype":"text/css"}, {"name":"treeview.css", "version":"1", "url":"servoyextra/treeview/css/treeview.css", "mimetype":"text/css"}],
	"model":
	{
	    "jsDataSet": {"type" :"dataset", "includeColumnNames": true, "columnTypes":{ "icon" : "media" }}
	},	
	"handlers":
	{
		"onNodeClicked": "function",
		"onNodeExpanded": "function",
	    "onNodeCollapsed": "function",
	    "onNodeSelected": "function"
	},
	"api":
	{
        "setDataSet": {
			"parameters":[
							{                                                                 
							"name":"jsDataSet",
							"type": {"type" :"dataset", "includeColumnNames": true, "columnTypes":{ "icon" : "media" }}
		                	}             
						 ]
        },
        
      	"refresh": {
      		"parameters":[
      						{
      						"name":"restoreExpandedNodes",
      						"type":"boolean"
      						}
      					]
      	},
      	
      	"expandNode": {
      		"parameters":[
      						{
      						"name":"nodeId",
      						"type":"object"
      						}
      					]
      	},
      	
		"isNodeExpanded": {
      		"parameters":[
      						{
      						"name":"nodeId",
      						"type":"object"
      						}
      					],
      		"returns": "boolean"
      	},
      	
     	"collapseNode": {
      		"parameters":[
      						{
      						"name":"nodeId",
      						"type":"object"
      						}
      					]
      	},
 
      	"setSelectedNode": {
      		"parameters":[
      						{
      						"name":"nodeId",
      						"type":"object"
      						}
      					]
      	},
      	
     	"getSeletedNode": {
      		"returns": "object"
      	},
      	
     	"getChildNodes": {
      		"parameters":[
      						{
      						"name":"nodeId",
      						"type":"object"
      						}
      					],     	
      		"returns": "object"
      	},
      	
     	"getParentNode": {
      		"parameters":[
      						{
      						"name":"nodeId",
      						"type":"object"
      						}
      					],     	
      		"returns": "object"
      	},      	
      	
     	"getNodeLevel": {
      		"parameters":[
      						{
      						"name":"nodeId",
      						"type":"object"
      						}
      					],     	
      		"returns": "int"
      	},      	      	
      	
		"getRootNodes": {   	
      		"returns": "object"
      	}
	}
}
