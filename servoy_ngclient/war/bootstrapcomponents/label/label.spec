{
	"name": "bootstrapcomponents-label",
	"displayName": "Label",
	"version": 1,
	"icon": "servoydefault/label/text.gif",
	"definition": "bootstrapcomponents/label/label.js",
	"libraries": [],
	"model":
	{
			"styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :["label","label-default","label-primary","label-success","label-info","label-warning","label-danger"]},
	        "text" : {"type":"tagstring" , "default":"Label"}
	},
	"handlers":
	{
	        "onActionMethodID" : "function", 
	        "onDoubleClickMethodID" : "function", 
	        "onRightClickMethodID" : "function" 
	},
	"api":
	{
	        
	}
	 
}