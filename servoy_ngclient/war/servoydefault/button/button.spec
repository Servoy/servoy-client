{
	"name": "servoydefault-button",
	"displayName": "Button",
	"icon": "servoydefault/button/button.gif",
	"definition": "servoydefault/button/button.js",
	"libraries": [],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "dataProviderID" : { "type":"dataprovider", "scope" :"design", "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}}, 
	        "enabled" : {"type":"boolean", "default":true}, 
	        "fontType" : "font", 
	        "foreground" : "color", 
	        "format" : {"for":"dataProviderID" , "type" :"format"}, 
	        "horizontalAlignment" : {"type" :"int", "scope" :"design", "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}], "default" : 0}, 
	        "imageMediaID" : "media", 
	        "location" : "point", 
	        "margin" : {"type" :"insets", "scope" :"design"}, 
	        "mediaOptions" : {"type" :"mediaoptions", "scope" :"design"}, 
	        "mnemonic" : "string", 
	        "rolloverCursor" : {"type" :"int", "scope" :"design"}, 
	        "rolloverImageMediaID" : {"type" : "media", "scope" :"design"}, 
	        "size" : {"type" :"dimension",  "default" : {"width":80, "height":20}}, 
	        "styleClass" : { "type" :"styleclass", "scope" :"design", "values" :["btn","btn-default","btn-lg","btn-sm","btn-xs"]}, 
	        "tabSeq" : {"type" :"tabseq", "scope" :"design"}, 
	        "text" : "tagstring", 
	        "textRotation" : {"type" :"int", "scope" :"design", "values" :[0,90,180,270]}, 
	        "toolTipText" : "tagstring", 
	        "transparent" : "boolean", 
	        "verticalAlignment" : {"type" :"int", "scope" :"design", "values" :[{"TOP":1}, {"CENTER":0} ,{"BOTTOM":3}], "default" : 0}, 
	        "visible" : {"type":"boolean", "default":true} 
	},
	"handlers":
	{
	        "onActionMethodID" : "function", 
	        "onDoubleClickMethodID" : "function", 
	        "onRightClickMethodID" : "function" 
	},
	"api":
	{
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
	        "requestFocus": {
				"parameters":[
								{                                                                 
 								"name":"mustExecuteOnFocusGainedMethod",
								"type":"boolean",
			            		"optional":true
			            		}             
							 ]
	        }
	}
	 
}