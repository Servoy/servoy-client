{
	"name": "mypagingcomponent",
	"displayName": "My Component with paging behavior",
	"definition": "mypagingcomponent.js",
	"libraries": [],
	"model":
	{
	        "myfoundset": { "type": "foundset", "dynamicDataproviders": "true", "pushToServer": "allow", "initialPreferredViewPortSize": 12, "sendSelectionViewportInitially": true },
            "fakePageSize": { "type": "foundsetInitialPageSize", "for": "puppy" },
            "pageSize": { "type": "foundsetInitialPageSize", "for": "myfoundset" }
	}
} 
