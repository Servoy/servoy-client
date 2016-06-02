{
	"name": "bootstrapcomponents-checkbox",
	"displayName": "CheckBox",
	"version": 1,
	"icon": "servoydefault/check/CHECKBOX16.png",
	"definition": "bootstrapcomponents/checkbox/checkbox.js",
	"libraries": [],
	"model":
	{
	        "dataProviderID" : { "type":"dataprovider", "pushToServer": "allow", "tags": { "scope" :"design" }, "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}},
	        "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["dataProviderID","onActionMethodID","onDataChangeMethodID"] },
	        "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "default":"checkbox"},
	        "text" : { "type" : "tagstring" ,"default": "Checkbox" },
			"tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }},
			"valuelistID" : { "type" : "valuelist", "tags": { "scope" :"design" }, "for": "dataProviderID"},
	        "visible" : "visible"
	},
	"handlers":
	{
	         "onActionMethodID" : {

	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSEvent"
								}
							 ]
	        },
	        "onDataChangeMethodID" : {
	          "returns": "boolean",

	        	"parameters":[
								{
						          "name":"oldValue",
								  "type":"${dataproviderType}"
								},
								{
						          "name":"newValue",
								  "type":"${dataproviderType}"
								},
								{
						          "name":"event",
								  "type":"JSEvent"
								}
							 ]
	        }
	},
	"api":
	{

	}

}
