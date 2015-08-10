{
	"name": "mycomponent",
	"displayName": "My Component",
	"definition": "mycomponent.js",
	"libraries": [],
	"model":
	{
		"background": {"type":"color", "pushToServer":"shallow"},
		"objectT": "mytype007",
		"arrayT": "mytype007[]"
	},
	"types": {
		"mytype007": {
			"name": "string",
			"text": "string",
			"active": "activeType[]",
			"foreground": "color",
			"size": "dimension",
			"mnemonic": "string"
		},
		"activeType": {
			"field": "int",
			"percent": "double"
		}
	}
} 
