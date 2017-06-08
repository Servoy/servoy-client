{
	"name": "servoycore-portal",
	"displayName": "Portal",
	"version": 1,
	"icon": "servoycore/portal/portal.png",
	"definition": "servoycore/portal/portal.js",
	"serverscript": "servoycore/portal/portal_server.js",
	"libraries": [{"name":"ui-grid", "version":"v3.0.0-rc.12", "url":"servoycore/portal/js/ui-grid.js", "mimetype":"text/javascript"},
				{"name":"ui-grid", "version":"v3.0.0-rc.12", "url":"servoycore/portal/css/ui-grid.min.css", "mimetype":"text/css"},
				{"name":"svy-portal", "version":"1", "url":"servoycore/portal/portal.css", "mimetype":"text/css"}],
	"model":
	{
	        "background" : "color", 
	        "borderType" : { "type" : "border", "stringformat" : true }, 
	        "childElements" : { "type" : "component[]", "pushToServer" : "allow",
	        					"tags" : { "scope" : "private" },
								"elementConfig" : {
									"forFoundset" : "relatedFoundset",
									"tags" : { "addToElementsScope" : "true" }
								}
	        }, 
	        "headers" : { "type" : "component[]", "pushToServer": "allow", "tags" : {"scope": "private"} }, 
	        "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["onDragEndMethodID","onDragMethodID","onDragOverMethodID","onDropMethodID"] }, 
	        "findmode" : { "type":"findmode", "tags":{"scope":"private"}, "for" : { "readOnly":false}}, 
	        "foreground" : "color", 
	        "headerHeight" : {"type" :"int",  "default" : 32}, 
	        "initialSort" : "string", 
	        "intercellSpacing" : "dimension", 
	        "location" : "point", 
	        "multiLine" : "boolean", 
	        "readOnly" : { "type" : "protected", "for" : ["readOnly"]}, 
	        "readOnlyMode" :{"type":"protected", "for": ["readOnlyMode"], "default": null},
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
	        "onDragEndMethodID" : {
	         	
	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSDNDEvent"
								} 
							 ]
	        }, 
	        "onDragMethodID" : {
	          "returns": "int", 
	         	
	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSDNDEvent"
								} 
							 ]
	        }, 
	        "onDragOverMethodID" : {
	          "returns": "boolean", 
	         	
	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSDNDEvent"
								} 
							 ]
	        }, 
	        "onDropMethodID" : {
	          "returns": "boolean", 
	         	
	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSDNDEvent"
								} 
							 ]
	        } 
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