{
	"name": "servoydefault-splitpane",
	"displayName": "Split Pane",
	"version": 1,
	"icon": "servoydefault/splitpane/split.gif",
	"definition": "servoydefault/splitpane/splitpane.js",
	"serverscript": "servoydefault/splitpane/splitpane_server.js",
	"libraries": [{"name":"bg-splitter", "version":"1", "url":"servoydefault/splitpane/bg-splitter/js/splitter.js", "mimetype":"text/javascript"},{"name":"bg-splitter", "version":"1", "url":"servoydefault/splitpane/bg-splitter/css/style.css", "mimetype":"text/css"}],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "divLocation" : { "type": "double", "pushToServer": "shallow", "default": -1 }, 
	        "divSize" : { "type": "int", "default": 5 }, 
	        "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["onChangeMethodID","onTabChangeMethodID"] }, 
	        "fontType" : {"type":"font","stringformat":true}, 
	        "foreground" : "color", 
	        "horizontalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "location" : "point", 
	        "pane1MinSize" : {"type":"int","default":30}, 
	        "pane2MinSize" : {"type":"int","default":30}, 
	        "readOnly" : { "type": "protected", "for": ["onChangeMethodID","onTabChangeMethodID"] }, 
	        "resizeWeight" : {"type":"double","default":0}, 
	        "selectedTabColor" : "color", 
	        "size" : {"type" :"dimension",  "default" : {"width":300, "height":300}}, 
	        "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :[]}, 
	        "tabOrientation" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"default" :0}, {"TOP":1}, {"HIDE":-1}]}, 
	        "tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }}, 
	        "tabs" : {"type":"tab[]", "pushToServer": "allow", "droppable":true}, 
	        "transparent" : "boolean", 
	        "visible" : "visible" 
	},
	"handlers":
	{
	        "onChangeMethodID" : {
	         	
	        	"parameters":[
								{
						          "name":"previousIndex",
								  "type":"int"
								}, 
								{
						          "name":"event",
								  "type":"JSEvent"
								} 
							 ]
	        } 
	},
	"api":
	{
	        "addTab": {
	            "returns": "boolean",
				"parameters":[
								{                                                                 
 								"name":"vargs",
								"type":"object []"
			                	}             
							 ]
	
	        },
	        "getContinuousLayout": {
	            "returns": "boolean"
	        },
	        "getDividerLocation": {
	            "returns": "double"
	        },
	        "getDividerSize": {
	            "returns": "int"
	        },
	        "getFormName": {
	            "returns": "string"
	        },
	        "getHeight": {
	            "returns": "int"
	        },
	        "getLeftForm": {
	            "returns": "formscope"
	        },
	        "getLeftFormMinSize": {
	            "returns": "int"
	        },
	        "getLocationX": {
	            "returns": "int"
	        },
	        "getLocationY": {
	            "returns": "int"
	        },
	        "getMaxTabIndex": {
	            "returns": "int"
	        },
	        "getMnemonicAt": {
	            "returns": "string",
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "getResizeWeight": {
	            "returns": "double"
	        },
	        "getRightForm": {
	            "returns": "formscope"
	        },
	        "getRightFormMinSize": {
	            "returns": "int"
	        },
	        "getTabFGColorAt": {
	            "returns": "string",
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "getTabFormNameAt": {
	            "returns": "string",
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "getTabNameAt": {
	            "returns": "string",
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "getTabRelationNameAt": {
	            "returns": "string",
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "getTabTextAt": {
	            "returns": "string",
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "getWidth": {
	            "returns": "int"
	        },
	        "isTabEnabledAt": {
	            "returns": "boolean",
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "removeAllTabs": {
	            "returns": "boolean"
	        },
	        "removeTabAt": {
	            "returns": "boolean",
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "setContinuousLayout": {
				"parameters":[
								{                                                                 
 								"name":"b",
								"type":"boolean"
			                	}             
							 ]
	
	        },
	        "setDividerLocation": {
				"parameters":[
								{                                                                 
 								"name":"location",
								"type":"double"
			                	}             
							 ]
	
	        },
	        "setDividerSize": {
				"parameters":[
								{                                                                 
 								"name":"size",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "setLeftForm": {
	            "returns": "boolean",
				"parameters":[
								{                                                                 
 								"name":"form",
								"type":"object"
			                	},
             					{                                                                 
 								"name":"relation",
								"type":"object",
			            		"optional":true
			            		}             
							 ]
	
	        },
	        "setLeftFormMinSize": {
				"parameters":[
								{                                                                 
 								"name":"minSize",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "setMnemonicAt": {
				"parameters":[
								{                                                                 
 								"name":"index",
								"type":"int"
			                	},
             					{                                                                 
 								"name":"text",
								"type":"string"
			                	}             
							 ]
	
	        },
	        "setResizeWeight": {
				"parameters":[
								{                                                                 
 								"name":"resizeWeight",
								"type":"double"
			                	}             
							 ]
	
	        },
	        "setRightForm": {
	            "returns": "boolean",
				"parameters":[
								{                                                                 
 								"name":"form",
								"type":"object"
			                	},
             					{                                                                 
 								"name":"relation",
								"type":"object",
			            		"optional":true
			            		}             
							 ]
	
	        },
	        "setRightFormMinSize": {
				"parameters":[
								{                                                                 
 								"name":"minSize",
								"type":"int"
			                	}             
							 ]
	
	        },
	        "setTabEnabledAt": {
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	},
             					{                                                                 
 								"name":"b",
								"type":"boolean"
			                	}             
							 ]
	
	        },
	        "setTabFGColorAt": {
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	},
             					{                                                                 
 								"name":"clr",
								"type":"string"
			                	}             
							 ]
	
	        },
	        "setTabTextAt": {
				"parameters":[
								{                                                                 
 								"name":"i",
								"type":"int"
			                	},
             					{                                                                 
 								"name":"text",
								"type":"string"
			                	}             
							 ]
	
	        }
	},
"types": {
  "tab": {
  		"name": "string",
  		"containsFormId": "form",
  		"text": "tagstring",
  		"relationName": "relation",
  		"active": "boolean",
  		"foreground": "color",
  		"disabled": "boolean",
  		"mnemonic": "string"
  	}
}
	 
}