{
	"name": "bootstrapcomponents-typeahead",
	"displayName": "Type Ahead ",
	"version": 1,
	"icon": "servoydefault/typeahead/bhdropdownlisticon.gif",
	"definition": "bootstrapcomponents/typeahead/typeahead.js",
	"libraries": [],
	"model":
	{
	        "dataProviderID" : { "type":"dataprovider","pushToServer": "allow", "tags": { "scope" :"design" }, "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}},
	        "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["dataProviderID","onActionMethodID","onDataChangeMethodID"] }, 
	        "format" : {"for":["valuelistID","dataProviderID"] , "type" :"format"},
	        "readOnly" : { "type": "protected", "blockingOn": true, "default": false,"for": ["dataProviderID","onDataChangeMethodID"] }, 
	        "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "default" : "form-control"}, 
	        "valuelistID" : { "type" : "valuelist", "tags": { "scope" :"design" }, "for": "dataProviderID", "default":"autoVL", "canOptimize":false, "pushToServer": "allow"},
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
	          "returns": "Boolean", 
	         	
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
	        } 
	},
	"api":
	{
	       
	}
	 
}