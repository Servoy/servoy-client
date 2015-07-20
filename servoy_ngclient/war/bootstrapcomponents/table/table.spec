{
	"name": "bootstrapcomponents-table",
	"displayName": "Table",
	"version": 1,
	"icon": "servoydefault/portal/portal.gif",
	"definition": "bootstrapcomponents/table/table.js",
	"libraries": [],
	"model":
	{
		"columns":  { "type":"column[]", "droppable": true },
		"foundset": { "type": "foundset", "pushToServer": "allow" },
		"styleClass" : { "type": "styleclass", "tags": { "scope": "design" }, "default": "table", "values": ["table", "table-striped", "table-bordered", "table-hover", "table-condensed"] }
	},
	"types": 
	{
		"column": 
		{
			"dataprovider": {	"type": "dataprovider",	"forFoundset": "foundset" },
			"format" : {"for":["valuelist","dataprovider"] , "type" :"format"}, 
			"headerText": {"type" :"string", "default" : "header"},
			"styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }},
			"valuelist" : { "type" : "valuelist", "tags": { "scope" :"design" }, "for": "dataprovider"} 
		}
	}
	 
}