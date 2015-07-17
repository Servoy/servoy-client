{
	"name": "servoydefault-label",
	"displayName": "Label",
	"version": 1,
	"icon": "servoydefault/label/text.gif",
	"definition": "servoydefault/label/label.js",
	"libraries": [],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "dataProviderID" : { "type":"dataprovider", "tags": { "scope": "design" }, "displayTagsPropertyName" : "displaysTags"}, 
	        "displaysTags" : { "type" : "boolean", "tags": { "scope" : "design" } }, 
	        "enabled" : { "type": "protected", "blockingOn": false, "default": true, "for": ["onActionMethodID","onDoubleClickMethodID","onRightClickMethodID"] }, 
	        "fontType" : {"type":"font","stringformat":true}, 
	        "foreground" : "color", 
	        "format" : {"for":["valuelistID","dataProviderID"] , "type" :"format"}, 
	        "horizontalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "imageMediaID" : "media", 
	        "labelFor" : "labelfor", 
	        "location" : "point", 
	        "margin" : {"type" :"insets", "tags": { "scope" :"design" }}, 
	        "mediaOptions" : {"type" :"mediaoptions", "tags": { "scope" :"design" }}, 
	        "mnemonic" : "string", 
	        "rolloverCursor" : {"type" :"int", "tags": { "scope" :"design" }}, 
	        "rolloverImageMediaID" : "media", 
	        "showFocus" : {"type":"boolean", "default":true}, 
	        "size" : {"type" :"dimension",  "default" : {"width":80, "height":20}}, 
	        "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :[]}, 
	        "tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }}, 
	        "text" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" , "tags": { "directEdit" : "true" } }, 
	        "textRotation" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[0,90,180,270]}, 
	        "toolTipText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "transparent" : "boolean", 
	        "verticalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"TOP":1}, {"CENTER":0} ,{"BOTTOM":3}], "default" : 0}, 
	        "visible" : "visible" 
	},
	"handlers":
	{
	        "onActionMethodID" : "function", 
	        "onDoubleClickMethodID" : "function", 
	        "onRightClickMethodID" : "function" 
	},
	"api":
	{
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
	        "getParameterValue": {
	            "returns": "string",
				"parameters":[
								{                                                                 
 								"name":"param",
								"type":"string"
			                	}             
							 ]
	
	        },
	        "getThumbnailJPGImage": {
	            "returns": "byte []",
				"parameters":[
								{                                                                 
 								"name":"width",
								"type":"int",
			            		"optional":true
			            		},
             					{                                                                 
 								"name":"height",
								"type":"int",
			            		"optional":true
			            		}             
							 ]
	
	        },
	        "getWidth": {
	            "returns": "int"
	        }
	}
	 
}