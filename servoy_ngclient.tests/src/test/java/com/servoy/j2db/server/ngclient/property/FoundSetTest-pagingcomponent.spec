{
	"name": "mypagingcomponent",
	"displayName": "My Component with paging behavior",
	"definition": "mypagingcomponent.js",
	"libraries": [],
	"model":
	{
	        "myfoundset": { "type": "foundset", "dynamicDataproviders": "true", "pushToServer": "allow", "initialPreferredViewPortSize": 15 },
            "fakePageSize1": { "type": "foundsetInitialPreferredViewportSize" },
            "fakePageSize2": { "type": "foundsetInitialPreferredViewportSize", "for": "puppy" },
            "pageSize": { "type": "foundsetInitialPreferredViewportSize", "for": "myfoundset" }
	}
} 
