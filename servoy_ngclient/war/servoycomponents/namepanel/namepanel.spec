{
	"name": "servoycomponents-namepanel",
	"displayName": "Name panel",
	"definition": "servoycomponents/namepanel/namepanel.js",
	"model":
	{
		"bgcolor": "color",
	    "firstNameDataprovider": "dataprovider",
	    "firstNameTabsequence": {"type": "tabseq","scope": "design"},
	    "lastNameDataprovider": "dataprovider",
	    "lastNameTabsequence": {"type": "tabseq","scope": "design"},
		"buttontext": {"type":"string", "default":"button"},
		"buttonClass": { "type":"styleclass", "values":["btn","btn-default","btn-lg","btn-sm","btn-xs"]},
		"cssClasses" : { "type":"styleclass", "default":"table"},
		"tooltiptext": "string",
		"readOnly": "boolean",
		"size" : {"type" :"dimension",  "default" : {"width":230, "height":120}}, 
	    "firstNameFormat": {"for":"firstNameDataprovider" , "type":"format"},
	    "testruntime": { "type": "string", "scope":"runtime"}
	},
	"handlers":
	{
	    "onAction": "function"
	}
}
