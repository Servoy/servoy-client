{
	"name": "bootstrapcomponents-label",
	"displayName": "Label",
	"version": 1,
	"icon": "servoydefault/label/text.gif",
	"definition": "bootstrapcomponents/label/label.js",
	"libraries": [],
	"model":
	{
			"labelFor" : { "type" : "string", "tags": { "scope" :"design" } },
			"styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :["label","label-default","label-primary","label-success","label-info","label-warning","label-danger"]},
	    	"text" : {"type":"tagstring" , "default":"Label"},
			"tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }},
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
	        "onDoubleClickMethodID" : {

	        	"parameters":[
								{
						          "name":"event",
								  "type":"JSEvent"
								}
							 ]
	        },
	        "onRightClickMethodID" : {

	        	"parameters":[
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
