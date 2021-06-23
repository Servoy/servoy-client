{
	"name": "mycomponent",
	"displayName": "My Component",
	"definition": "mycomponent.js",
	"libraries": [],
	"model":
	{
		"background": {"type":"color", "pushToServer":"shallow"},
		"objectT": "mytype007",
		"arrayT": "mytype007[]",
		"arraySkipNullsAtRuntime": { "type" : "objWith3Keys[]", "skipNullItemsAtRuntime": true, "elementConfig": { "setToNullAtRuntimeIfAnyOfTheseKeysAreNull": [ "a", "c" ] } },
		"normalArray": "objWith3Keys[]",
		"normalArrayWithConfig": { "type": "objWith3Keys[]", "elementConfig": {} },
		"defaultConversionsProp": "object",
		"stringArray": "string[]"
	},
	"types": {
		"mytype007": {
			"name": "string",
			"text": "string",
			"active": "activeType[]",
			"foreground": "color",
			"size": "dimension",
			"mnemonic": "string",
			"defaultConversionsSubProp": "object"
		},
		"activeType": {
			"field": "int",
			"percent": "double"
		},
		"objWith3Keys": {
			"a": "string",
			"b": "int",
			"c": "date"
		}
	}
} 
