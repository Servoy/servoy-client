{
	"name": "component1",
	"displayName": "Component 1",
	"definition": "component1.js",
	"libraries": [],
	"model":
	{
        "customType": "mytype",
        "arrayOfCustomType": "mytype[]",
        "arrayOfCustomTypeWithAllow": {"type": "mytype[]", "pushToServer": "allow" },
        "stringArray": { "type": "string[]", "default": [ "a", "b", "c"], "elementConfig": { "pushToServer": "shallow" } },
        "someDate": { "type": "date", "pushToServer": "allow" },
        "octWithCustomTypeAllow": { "type": "octWithCustomType", "pushToServer": "allow" },
        "arrayWithOctAllowAndShallowEl": { "type": "octWithCustomType[]", "pushToServer": "allow", "elementConfig": { "pushToServer": "shallow" } }
	},
	"types": {
		"mytype": {
			"name": "string",
			"text": "tagstring",
			"form": "form",
			"someComponent": "component"
		},
		"octWithCustomType": {
			"someString": { "type": "string", "pushToServer": "reject" },
			"customType": { "type": "mytype", "pushToServer": "deep" },
			"ignoreMe": "int"
		}
	}
} 
