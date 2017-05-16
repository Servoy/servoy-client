{
	
	"name" : "ngclientutils",
	"displayName" : "Servoy NG Client Utils plugin",
	"version" : 1,
	"definition" : "servoy_ng_only_services/ngutils/ngutils.js",
	"serverscript" : "servoy_ng_only_services/ngutils/ngutils_server.js",
	"libraries" : [],
	
	"model" :
	{
		"VIEWPORT_MOBILE_DEFAULT" : { "type": "int", "default": 1 },
		"VIEWPORT_MOBILE_DENY_ZOOM" : { "type": "int", "default": 2 },
		"VIEWPORT_MOBILE_DENY_ZOOM_OUT" : { "type": "int", "default": 3 },
		"VIEWPORT_MOBILE_DENY_ZOOM_IN" : { "type": "int", "default": 4 },
		"contributedTags" : "tag[]",
		"styleclasses" : {"type":"object", "pushToServer": "shallow", "tags": { "scope": "private" }}, 
		
	},
	
	"api" :
	{
		"getUserAgent" : {
			"parameters" : [ ],
			"returns" :"string"
		},
		"setOnUnloadConfirmationMessage" : {
			"parameters" : [ { "name" : "message", "type" : "string" } ]
		},
		"setOnUnloadConfirmation" : {
			"parameters" : [ { "name" : "showConfirmation", "type" : "boolean" } ]
		},
		"setViewportMetaDefaultForMobileAwareSites" : {
			"parameters" : [ ]
		},
		"setViewportMetaForMobileAwareSites" : {
			"parameters" : [ { "name" : "viewportDefType", "type" : "int" } ]
		},
		"replaceHeaderTag" : {
			"parameters" : [ { "name" : "tagName", "type" : "string" },
			                 { "name" : "attrNameToFind", "type" : "string" },
			                 { "name" : "attrValueToFind", "type" : "string" },
			                 { "name" : "newTag", "type" : "tag" } ],
			"returns": "tag"
		},
		"addFormStyleClass" :{
			"parameters" : [ { "name" : "formname", "type" : "string" },
							 { "name" : "styleclass", "type" : "string" }]
		},
		"removeFormStyleClass" :{
			"parameters" : [ { "name" : "formname", "type" : "string" },
							 { "name" : "styleclass", "type" : "string" }]
		},
		"getFormStyleClass" :{
			"parameters" : [ { "name" : "formname", "type" : "string" }],
			"returns": "string"
		}
	},
	
	"types" : {
		"tag" : {
			"tagname" : "string",
			"attrs" : "attribute[]"
		},
		"attribute" : {
			"name" : "string",
			"value" : "string"
		}
	}
}
