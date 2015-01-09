{
	"name": "servoydefault-textfield",
	"displayName": "Text Field",
	"version": 1,
	"icon": "servoydefault/textfield/textinput.png",
	"definition": "servoydefault/textfield/textfield.js",
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
	        "format" : {"for":"dataProviderID" , "type" :"format"}, 
	        "horizontalAlignment" : {"type" :"int", "scope" :"design", "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "location" : "point", 
	        "margin" : {"type" :"insets", "scope" :"design"}, 
	        "placeholderText" : "tagstring", 
	        "selectOnEnter" : {"type" :"boolean", "scope" :"design"}, 
	        "size" : {"type" :"dimension",  "default" : {"width":140, "height":20}}, 
	        "styleClass" : { "type" :"styleclass", "scope" :"design", "values" :["form-control", "input-sm", "svy-padding-xs"]}, 
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
	        "getSelectedText": {
	            "returns": "string"
	        },
	        "replaceSelectedText": {
				"parameters":[
								{                                                                 
 								"name":"s",
								"type":"string"
			                	}             
							 ]
	        },
	        "requestFocus": {
				"parameters":[
								{                                                                 
 								"name":"mustExecuteOnFocusGainedMethod",
								"type":"boolean",
			            		"optional":true
			            		}             
							 ]
	        },
	        "selectAll": {
	
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