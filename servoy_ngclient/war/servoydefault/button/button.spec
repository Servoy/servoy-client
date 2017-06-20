{
	"name": "servoydefault-button",
	"displayName": "Button",
	"version": 1,
	"icon": "servoydefault/button/button.png",
	"definition": "servoydefault/button/button.js",
	"libraries": [],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "dataProviderID" : { "type":"dataprovider", "tags": { "scope": "design" }, "displayTagsPropertyName" : "displaysTags"}, 
	        "displaysTags" : { "type" : "boolean", "tags": { "scope" : "design" } }, 
	        "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["onActionMethodID","onDoubleClickMethodID","onRightClickMethodID"] }, 
	        "fontType" : {"type":"font","stringformat":true}, 
	        "foreground" : "color", 
	        "format" : {"for":["dataProviderID"] , "type" :"format"}, 
	        "horizontalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}], "default" : 0}, 
	        "imageMediaID" : "media", 
	        "location" : {"type" :"point", "pushToServer": "deep"}, 
	        "margin" : {"type" :"insets", "tags": { "scope" :"design" }}, 
	        "mediaOptions" : {"type" :"mediaoptions", "tags": { "scope" :"design" }}, 
	        "mnemonic" : "string", 
	        "rolloverCursor" : {"type" :"int", "tags": { "scope" :"design" }}, 
	        "rolloverImageMediaID" : "media", 
	        "showFocus" : {"type":"boolean", "default":true}, 
	        "size" : {"type" :"dimension",  "default" : {"width":80, "height":20}, "pushToServer": "deep"}, 
	        "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :["btn","btn-default","btn-lg","btn-sm","btn-xs"]}, 
	        "tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }}, 
	        "text" : { "type" : "tagstring", "initialValue":"button", "displayTagsPropertyName" : "displaysTags" , "tags": { "directEdit" : "true" } }, 
	        "textRotation" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[0,90,180,270],  "default" : 0}, 
	        "toolTipText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "transparent" : "boolean", 
	        "verticalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"TOP":1}, {"CENTER":0} ,{"BOTTOM":3}], "default" : 0}, 
	        "visible" : "visible" 
	},
	"handlers":
	{
	        "onActionMethodID" : {
	         	
	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSEvent"
								} 
							 ]
	        }, 
	        "onDoubleClickMethodID" : {
	         	
	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSEvent"
								} 
							 ]
	        }, 
	        "onRightClickMethodID" : {
	         	
	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSEvent"
								} 
							 ]
	        } 
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
	        },
	        "requestFocus": {
				"parameters":[
								{                                                                 
 								"name":"mustExecuteOnFocusGainedMethod",
								"type":"boolean",
			            		"optional":true
			            		}             
							 ],
				"delayUntilFormLoads": true,
			"discardPreviouslyQueuedSimilarCalls": true

	        }
	}
	 
}