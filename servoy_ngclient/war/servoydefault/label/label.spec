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
	        "dataProviderID" : { "type":"dataprovider", "scope" :"design", "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}}, 
	        "directEditPropertyName" : {"type" :"string",  "default" : "text"}, 
	        "displaysTags" : { "type" : "boolean", "scope" : "design" }, 
	        "enabled" : { "type": "protected", "blockingOn": false, "default": true }, 
	        "fontType" : {"type":"font","stringformat":true}, 
	        "foreground" : "color", 
	        "format" : {"for":"dataProviderID" , "type" :"format"}, 
	        "horizontalAlignment" : {"type" :"int", "scope" :"design", "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "imageMediaID" : "media", 
	        "labelFor" : "bean", 
	        "location" : "point", 
	        "margin" : {"type" :"insets", "scope" :"design"}, 
	        "mediaOptions" : {"type" :"mediaoptions", "scope" :"design"}, 
	        "mnemonic" : "string", 
	        "rolloverCursor" : {"type" :"int", "scope" :"design"}, 
	        "rolloverImageMediaID" : {"type" : "media", "scope" :"design"}, 
	        "size" : {"type" :"dimension",  "default" : {"width":80, "height":20}}, 
	        "styleClass" : { "type" :"styleclass", "scope" :"design", "values" :[]}, 
	        "tabSeq" : {"type" :"tabseq", "scope" :"design"}, 
	        "text" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "textRotation" : {"type" :"int", "scope" :"design", "values" :[0,90,180,270]}, 
	        "toolTipText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "transparent" : "boolean", 
	        "verticalAlignment" : {"type" :"int", "scope" :"design", "values" :[{"TOP":1}, {"CENTER":0} ,{"BOTTOM":3}], "default" : 0}, 
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
	        "getLabelForElementName": {
	            "returns": "string"
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
	        }
	}
	 
}