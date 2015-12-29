{
	
	"name" : "htmlHeaders",
	"displayName" : "Servoy HTMLHeaders plugin",
	"version" : 1,
	"definition" : "servoy_ng_only_services/htmlHeaders/htmlHeaders.js",
	"serverscript" : "servoy_ng_only_services/htmlHeaders/htmlHeaders_server.js",
	"libraries" : [],
	
	"model" :
	{
		"VIEWPORT_MOBILE_DEFAULT" : { "type": "int", "default": 1 },
		"VIEWPORT_MOBILE_DENY_ZOOM" : { "type": "int", "default": 2 },
		"VIEWPORT_MOBILE_DENY_ZOOM_OUT" : { "type": "int", "default": 3 },
		"VIEWPORT_MOBILE_DENY_ZOOM_IN" : { "type": "int", "default": 4 },
		"contributedTags" : "tag[]"
	},
	
	"api" :
	{
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
		"applyHeaderTags" : {
			"parameters" : [ ]
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
