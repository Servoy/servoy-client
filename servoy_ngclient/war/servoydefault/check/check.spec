{
	"name": "servoydefault-check",
	"displayName": "Check",
	"version": 1,
	"icon": "servoydefault/check/CHECKBOX16.png",
	"definition": "servoydefault/check/check.js",
	"libraries": [],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "dataProviderID" : { "type":"dataprovider", "scope" :"design", "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}}, 
	        "editable" : { "type": "protected", "blockingOn": false, "default": true },
	        "enabled" : { "type": "protected", "blockingOn": false, "default": true },
	        "fontType" : {"type":"font","stringformat":true}, 
	        "foreground" : "color", 
	        "horizontalAlignment" : {"type" :"int", "scope" :"design", "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "location" : "point", 
	        "margin" : {"type" :"insets", "scope" :"design"}, 
	        "placeholderText" : "tagstring", 
	        "size" : {"type" :"dimension",  "default" : {"width":140, "height":20}}, 
	        "styleClass" : { "type" :"styleclass", "scope" :"design", "values" :["checkbox"]}, 
	        "tabSeq" : {"type" :"tabseq", "scope" :"design"}, 
	        "text" : "tagstring", 
	        "toolTipText" : "tagstring", 
	        "transparent" : "boolean", 
	        "valuelistID" : { "type" : "valuelist", "scope" :"design", "for": "dataProviderID"}, 
	        "visible" : "visible"
	},
	"handlers":
	{
	        "onActionMethodID" : "function", 
	        "onDataChangeMethodID" : "function", 
	        "onFocusGainedMethodID" : "function", 
	        "onFocusLostMethodID" : "function", 
	        "onRightClickMethodID" : "function" 
	},
	"api":
	{
	        "requestFocus": {
				"parameters":[
								{                                                                 
 								"name":"mustExecuteOnFocusGainedMethod",
								"type":"boolean",
			            		"optional":true
			            		}             
							 ]
	        },
	        "setValueListItems": {
				"parameters":[
								{                                                                 
 								"name":"value",
								"type":"dataset"
			                	}             
							 ]
	        }
	}
	 
}