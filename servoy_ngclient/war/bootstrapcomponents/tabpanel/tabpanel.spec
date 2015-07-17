{
	"name": "bootstrapcomponents-tabpanel",
	"displayName": "TabPanel",
	"version": 1,
	"icon": "servoydefault/tabpanel/tabs.gif",
	"definition": "bootstrapcomponents/tabpanel/tabpanel.js",
	"libraries": [],
	"model":
	{
			"containerStyleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }},
			"tabs" : {"type":"tab[]", "pushToServer": "allow","droppable":true},
			"styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }},
			"height" : {"type":"int", "default":"500"}
	},
	"handlers":
	{
	
	},
	"api":
	{
	        
	},
	"types": {
  	 "tab": {
  	 	"active": {"type":"boolean","default": false,"tags": { "scope" :"private" }},
  		"containedForm": "form",
  		"text": {"type":"tagstring","default":"tab"},
  		"relationName": "relation"
  		}
	}
	 
}