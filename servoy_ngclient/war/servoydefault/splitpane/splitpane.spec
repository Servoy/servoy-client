{
	"name": "svy-splitpane",
	"displayName": "Split Pane",
	"icon": "servoydefault/splitpane/split.gif",
	"definition": "servoydefault/splitpane/splitpane.js",
	"libraries": [{"name":"bg-splitter.js", "version":"1", "url":"servoydefault/splitpane/bg-splitter/js/splitter.js", "mimetype":"text/javascript"},{"name":"bg-splitter.css", "version":"1", "url":"servoydefault/splitpane/bg-splitter/css/style.css", "mimetype":"text/css"}],
	"model":
	{
	        "background" : "color", 
	        "borderType" : "border", 
	        "enabled" : {"type":"boolean", "default":true}, 
	        "fontType" : "font", 
	        "foreground" : "color", 
	        "horizontalAlignment" : {"type" :"int", "scope" :"design", "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "location" : "point", 
	        "readOnly" : "boolean", 
	        "selectedTabColor" : "color", 
	        "size" : {"type" :"dimension",  "default" : {"width":300, "height":300}}, 
	        "styleClass" : { "type" :"styleclass", "scope" :"design", "values" :[]}, 
	        "tabOrientation" : {"type" :"int", "scope" :"design", "values" :[{"default" :0}, {"TOP":1}, {"HIDE":-1}]}, 
	        "tabSeq" : {"type" :"tabseq", "scope" :"design"}, 
	        "tabs" : "tab[]", 
	        "transparent" : "boolean", 
	        "visible" : {"type":"boolean", "default":true} 
	},
	"handlers":
	{
	        "onChangeMethodID" : "function", 
	        "onTabChangeMethodID" : "function" 
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
	        "getLeftForm": {
	            "returns": "formscope"
	        },
	        "getLeftFormMinSize": {
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
			            		"optional":"true"
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
			            		"optional":"true"
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
  	"model": {
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
	 
}