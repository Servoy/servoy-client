{
	"name": "bootstrapcomponents-tabpanel",
	"displayName": "TabPanel",
	"version": 1,
	"icon": "servoydefault/tabpanel/tabs.gif",
	"definition": "bootstrapcomponents/tabpanel/tabpanel.js",
	"libraries": [],
	"model":
	{
			"containerStyleClass" : { "type" :"styleclass"},
			"tabs" : {"type":"tab[]", "pushToServer": "allow","droppable":true},
			"styleClass" : { "type" :"styleclass"},
			"height" : {"type":"int", "default":"500"},
			"tabIndex" : {"type":"int", "pushToServer": "allow", "tags": { "scope" :"runtime" }},
			"tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }},
	    	"visible" : "visible"
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
  		"imageMediaID": "media",
  		"text": {"type":"tagstring","default":"tab"},
  		"relationName": "relation"
  		}
	}

}
