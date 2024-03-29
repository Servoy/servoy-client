{
	"name": "foundset_manager",
	"displayName": "FoundSet Manager",
	"version": 1,
	"definition": "servoyservices/foundset_manager/foundsetManager.js",
	"libraries": [],
	"model":
	{
		"foundsets" : "foundsetinfo[]"
	},
	"api": 
	{
		"getFoundSet": 
		{
			"parameters": 
			[
				{
					"name": "foundsethash",
					"type": "string"
				},
				{
					"name": "dataproviders",
					"type": "object"
				},
				{
					"name": "sort",
					"type": "string"
				}
			]
		},		
		"getRelatedFoundSetHash": 
		{
			"returns": "int",
			"parameters": 
			[
				{
					"name": "foundsethash",
					"type": "string"
				},
				{
					"name": "rowid",
					"type": "string"
				},
				{
					"name": "relation",
					"type": "string"
				}
			]
		},
		
		"updateFoundSetRow": 
		{
			"returns": "boolean",
			"parameters": 
			[
				{
					"name": "foundsethash",
					"type": "string"
				},
				{
					"name": "rowid",
					"type": "string"
				},
				{
					"name": "dataproviderid",
					"type": "string"
				},
				{
					"name": "value",
					"type": "object"
				}				
			]
		}
	},
	"types": {
	  "foundsetinfo": {
	  		"foundset": "foundset",
	  		"foundsethash": "string",
	  		"dataproviders": "string[]",
	  		"childrelationinfo": "object" 
	  }
	}	
}