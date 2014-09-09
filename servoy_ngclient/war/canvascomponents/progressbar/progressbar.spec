{
	"name": "canvas-progressbar",
	"displayName": "Progressbar",
	"definition": "canvascomponents/progressbar/progressbar.js",
	"libraries": ["canvascomponents/progressbar/progressbar.css", "http://preview.jumpstartthemes.com/canvas-admin/js/libs/jquery-ui-1.9.2.custom.min.js","http://preview.jumpstartthemes.com/canvas-admin/js/libs/css/ui-lightness/jquery-ui-1.9.2.custom.css"],
	"model":
	{
		"label": {"type":"string"},
		"max": {"type":"long", "default":100},
	    "progressbarText":{"type":"string"},
	    "styleClass" : { "type" :"styleclass", "scope" :"design", "values" :[]},
	    "value": {"type":"float", "default":0}
	},
}