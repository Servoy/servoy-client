{
	"name": "bootstrapcomponents-choicegroup",
	"displayName": "Choice Group",
	"version": 1,
	"definition": "bootstrapcomponents/choicegroup/choicegroup.js",
	"libraries": [],
	"model":
	{
	        "dataProviderID" : { "type":"dataprovider", "pushToServer": "allow","tags": { "scope" :"design" }, "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}},
	        "inputType" : {"type" : "string" , "default" : "checkbox" ,"tags": { "scope" :"design" }, "values" : ["checkbox","radio"]}, 
	        "styleClass" : { "type" :"styleclass", "default" : "checkbox" , "tags": { "scope" :"design" }, "values" :["checkbox", "radio"]}, 
	        "valuelistID" : { "type" : "valuelist", "tags": { "scope" :"design" }, "for": "dataProviderID"} 
	},
	"handlers":
	{
	        "onActionMethodID" : "function", 
	        "onDataChangeMethodID" : "function"
	},
	"api":
	{
	     
	}
	 
}