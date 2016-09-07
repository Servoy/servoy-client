{
	"name": "servoydefault-textarea",
	"displayName": "Text Area",
	"version": 1,
	"icon": "servoydefault/textarea/TEXTAREA16.png",
	"definition": "servoydefault/textarea/textarea.js",
	"libraries": [],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "dataProviderID" : { "type":"dataprovider", "pushToServer": "allow", "tags": { "scope": "design" }, "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}, "displayTagsPropertyName" : "displaysTags"}, 
	        "displaysTags" : { "type" : "boolean", "tags": { "scope" : "design" } }, 
	        "editable" : { "type": "protected", "blockingOn": false, "default": true,"for": ["dataProviderID","onDataChangeMethodID"] }, 
	        "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["dataProviderID","onActionMethodID","onDataChangeMethodID","onFocusGainedMethodID","onFocusLostMethodID","onRightClickMethodID"] }, 
	        "findmode" : { "type":"findmode", "tags":{"scope":"private"}, "for" : {"editable":true}}, 
	        "fontType" : {"type":"font","stringformat":true}, 
	        "foreground" : "color", 
	        "format" : {"for":["dataProviderID"] , "type" :"format"}, 
	        "horizontalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "location" : {"type" :"point", "pushToServer": "deep"}, 
	        "margin" : {"type" :"insets", "tags": { "scope" :"design" }}, 
	        "placeholderText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "readOnly" : { "type" : "readOnly", "oppositeOf" : "editable"}, 
	        "scrollbars" : {"type" :"scrollbars", "tags": { "scope" :"design" }}, 
	        "size" : {"type" :"dimension",  "default" : {"width":140, "height":140}, "pushToServer": "deep"}, 
	        "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :["form-control", "input-sm", "svy-padding-xs"]}, 
	        "tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }}, 
	        "text" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "toolTipText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
	        "transparent" : "boolean", 
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
	        "onDataChangeMethodID" : {
	          "returns": "boolean", 
	         	
	        	"parameters":[
								{
						          "name":"oldValue",
								  "type":"${dataproviderType}"
								}, 
								{
						          "name":"newValue",
								  "type":"${dataproviderType}"
								}, 
								{
						          "name":"event",
								  "type":"JSEvent"
								} 
							 ]
	        }, 
	        "onFocusGainedMethodID" : {
	         	
	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSEvent"
								} 
							 ]
	        }, 
	        "onFocusLostMethodID" : {
	         	
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
	        "getScrollX": {
	            "returns": "int"
	        },
	        "getScrollY": {
	            "returns": "int"
	        },
	        "getSelectedText": {
	            "returns": "string"
	        },
	        "getWidth": {
	            "returns": "int"
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
							 ],
				"delayUntilFormLoads": true,
			"discardPreviouslyQueuedSimilarCalls": true

	        },
	        "selectAll": {
				"delayUntilFormLoads": true,
			"discardPreviouslyQueuedSimilarCalls": true

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
	
	        }
	}
	 
}