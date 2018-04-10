{
	"name": "my-component",
	"displayName": "My Component",
	"definition": "mycomponent.js",
	"libraries": [],
	"model": {
		"myFoundset": { "type": "foundset" },
		"columns": { "type": "column[]" }
	},
	"types": {
		"column": {
			"myDataprovider": { "type": "dataprovider", "forFoundset": "myFoundset" },
			"myValuelist" : { "type": "valuelist", "for": "myDataprovider" }
		}
	}
} 
