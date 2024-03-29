{
	"name": "my-component",
	"displayName": "My Component",
	"definition": "mycomponent.js",
	"libraries": [],
	"model":
	{
	        "myfoundset": { "type": "foundset", "dataproviders": ["firstname", "lastname"], "initialPreferredViewPortSize": 15 },
	        "myfoundsetWithAllow": { "type": "foundset", "dataproviders": ["firstname", "lastname"], "pushToServer": "allow" ,"initialPreferredViewPortSize": 15 },
	        "datalinkedDPReject": { "type": "dataprovider", "forFoundset": "myfoundsetWithAllow" },
	        "datalinkedDPAllow": { "type": "dataprovider", "forFoundset": "myfoundsetWithAllow", "pushToServer": "allow" }
	}
} 
