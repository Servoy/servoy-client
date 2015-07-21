{
	"name": "servoydefault-portal",
	"displayName": "Portal",
	"version": 1,
	"icon": "servoydefault/portal/portal.gif",
	"definition": "servoydefault/portal/portal.js",
	"serverscript": "servoydefault/portal/portal_server.js",
	"libraries": [{"name":"ui-grid", "version":"v3.0.0-rc.12", "url":"servoydefault/portal/js/ui-grid.js", "mimetype":"text/javascript"},
				{"name":"ui-grid", "version":"v3.0.0-rc.12", "url":"servoydefault/portal/css/ui-grid.min.css", "mimetype":"text/css"},
				{"name":"svy-portal", "version":"1", "url":"servoydefault/portal/portal.css", "mimetype":"text/css"}],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "childElements" : { "type" : "component[]", "pushToServer": "allow", "elementConfig" : {"forFoundset": "relatedFoundset"}, "tags" : {"scope": "private"} }, 
	        "columnHeaders" : { "type" : "tagstring[]", "tags" : {"scope": "private"} }, 
	        "enabled" : { "type": "protected", "blockingOn": false, "default": true, "for": ["onDragEndMethodID","onDragMethodID","onDragOverMethodID","onDropMethodID"] }, 
	        "findmode" : { "type":"findmode", "tags":{"scope":"private"}, "for" : { "enabled":true}}, 
	        "foreground" : "color", 
	        "headerHeight" : {"type" :"int",  "default" : 32}, 
	        "headersClasses" : { "type" : "string[]", "tags" : {"scope": "private"} },
	        "headersAction" : { "type" : "function[]", "tags" : {"scope": "private"} }, 
	        "initialSort" : "string", 
	        "intercellSpacing" : "dimension", 
	        "location" : "point", 
	        "multiLine" : "boolean", 
	        "readOnly" : { "type" : "readOnly", "oppositeOf" : "enabled"}, 
	        "relatedFoundset" : {"type" :"foundset", "pushToServer": "allow"}, 
	        "reorderable" : "boolean", 
	        "resizable" : "boolean", 
	        "resizeble" : "boolean", 
	        "rowBGColorCalculation" : "string", 
	        "rowHeight" : "int", 
	        "scrollbars" : {"type" :"scrollbars", "tags": { "scope" :"design" }}, 
	        "showHorizontalLines" : "boolean", 
	        "showVerticalLines" : "boolean", 
	        "size" : {"type" :"dimension",  "default" : {"width":200, "height":200}}, 
	        "sortable" : "boolean", 
	        "styleClass" : "string", 
	        "tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }}, 
	        "transparent" : "boolean", 
	        "visible" : "visible" 
	},
	"handlers":
	{
	        "onDragEndMethodID" : "function", 
	        "onDragMethodID" : "function", 
	        "onDragOverMethodID" : "function", 
	        "onDropMethodID" : "function" 
	},
	"api":
	{
	        "deleteRecord": {
	
	        },
	        "duplicateRecord": {
				"parameters":[
								{                                                                 
 								"name":"addOnTop",
								"type":"boolean",
			            		"optional":true
			            		}             
							 ]
	
	        },
	        "getFormName": {
	            "returns": "string"
	        },
	        "getHeight": {
	            "returns": "int"
	        },
	        "getLocationX": {
	            "returns": "int"
	        },
	        "getLocationY": {
	            "returns": "int"
	        },
	        "getMaxRecordIndex": {
	            "returns": "int"
	        },
	        "getScrollX": {
	            "returns": "int"
	        },
	        "getScrollY": {
	            "returns": "int"
	        },
	        "getSelectedIndex": {
	            "returns": "int"
	        },
	        "getSortColumns": {
	            "returns": "string"
	        },
	        "getWidth": {
	            "returns": "int"
	        },
	        "newRecord": {
				"parameters":[
								{                                                                 
 								"name":"addOnTop",
								"type":"boolean",
			            		"optional":true
			            		}             
							 ]
	
	        },
	        "setScroll": {
				"parameters":[
								{                                                                 
 								"name":"x",
								"type":"int"
			                	},
             					{                                                                 
 								"name":"y",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "setSelectedIndex": {
				"parameters":[
								{                                                                 
 								"name":"index",
								"type":"int"
			                	}             
							 ]
	
	        }
	}
	 
}