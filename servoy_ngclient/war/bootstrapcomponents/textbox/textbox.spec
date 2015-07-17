{
	"name": "bootstrapcomponents-textbox",
	"displayName": "TextBox",
	"version": 1,
	"icon": "servoydefault/textfield/textinput.png",
	"definition": "bootstrapcomponents/textbox/textbox.js",
	"libraries": [],
	"model":
	{
			"dataProviderID" : { "type":"dataprovider", "pushToServer": "allow","tags": { "scope" :"design" }, "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}},
			"inputType" : {"type":"string" , "tags": { "scope" :"design" }, "default" : "text",  "values" :["text", "password"]},
			"placeholderText" : "tagstring",
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