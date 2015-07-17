{
	"name": "bootstrapcomponents-textarea",
	"displayName": "TextArea",
	"version": 1,
	"icon": "servoydefault/textarea/TEXTAREA16.png",
	"definition": "bootstrapcomponents/textarea/textarea.js",
	"libraries": [],
	"model":
	{
			"dataProviderID" : { "type":"dataprovider", "pushToServer": "allow","tags": { "scope" :"design" }, "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}},
			"styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "default": "form-control", "values" :["form-control", "input-sm"]} 
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
	        
	}
	 
}