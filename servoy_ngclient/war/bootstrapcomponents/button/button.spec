{
	"name": "bootstrapcomponents-button",
	"displayName": "Button",
	"version": 1,
	"icon": "servoydefault/button/button.gif",
	"definition": "bootstrapcomponents/button/button.js",
	"libraries": [],
	"model":
	{
			"imageStyleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }},
			"styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" },"default": "btn btn-default", "values" :["btn","btn-default","btn-lg","btn-sm","btn-xs"]},
	        "text" : {"type":"tagstring" , "default":"Button"},
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
	        
	}
	 
}