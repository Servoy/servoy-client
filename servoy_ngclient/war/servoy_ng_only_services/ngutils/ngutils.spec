{
	
	"name" : "ngclientutils",
	"displayName" : "Servoy NG Client Utils plugin",
	"version" : 1,
	"definition" : "servoy_ng_only_services/ngutils/ngutils.js",
	"libraries" : [],
	
	"model" :
	{
	},
	
	"api" :
	{
		"getUserAgent" : {
			"parameters" : [ ],
			"returns" :"string"
		},
		"setOnUnloadConfirmationMessage" : {
			"parameters" : [ { "name" : "message", "type" : "string" } ]
		}
	},
	
	"types" : {}
	
}
