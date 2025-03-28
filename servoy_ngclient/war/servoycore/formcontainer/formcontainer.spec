{
	"name": "servoycore-formcontainer",
	"displayName": "Form Container",
	"categoryName": "Form Containers",
	"version": 1,
	"icon": "servoycore/formcontainer/tab.png",
	"definition": "servoycore/formcontainer/formcontainer.js",
	"serverscript": "servoycore/formcontainer/formcontainer_server.js",
	"libraries": [{"name":"servoycore-formcontainer-css", "version":"1.0", "url":"servoycore/formcontainer/formcontainer.css", "mimetype":"text/css"}],
	"keywords": ["container"],
	"model":
	{
	       "animation" : {"type" : "string", "values": ["none","slide-up","slide-down", "slide-left", "slide-right", "rotate-y", "rotate-x"], "default": "none"},
			"containedForm": { "type" :"form"},
			"relationName": "relation",
			"waitForData" : { "type" :"boolean", "default":true, "deprecated" : "not used anymore, form visibility is handled server side now", "tags": { "doc": "When <code>true</code>, the form is rendered when all its latest data is loaded from the server. When <code>false</code>, the form is rendered faster, but could show stale data (not a problem when the form shown does not show dynamic data)" }},
			"styleClass" : { "type" :"styleclass"},
			"height" : {"type":"string", "default": "0", "tags": { "doc" : "Minimum height of the form container, should be used for responsive forms. Can be 100% (to take parent container height) or a number (in pixels)."}},
			"tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design","doc" : "Tab sequence number of form containers is used for all nested components in the main form." }},
	    	"visible" : "visible"
	},
	"handlers":
	{
	},
	"api":
	{
	},
	"internalApi" : {
		"onShow" : {},
		"onHide" : {}
	}
}
