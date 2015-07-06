{
	"name": "foundset_manager",
	"displayName": "FoundSet Manager",
	"version": 1,
	"definition": "servoyservices/foundset_manager/foundsetManager.js",
	"libraries": [],
	"model": {},
	"api": 
	{
		"getFoundSet": 
		{
			"returns": "foundset",
			"parameters": 
			[
				{
					"name": "foundsethash",
					"type": "int"
				}
			]
		},
	
		"getRelatedFoundSet": 
		{
			"returns": "foundset",
			"parameters": 
			[
				{
					"name": "foundsethash",
					"type": "int"
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
					"type": "int"
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
	}
}