{
	"name": "canvas-progressbar",
	"displayName": "Progressbar",
	"definition": "canvascomponents/progressbar/progressbar.js",
	"libraries": [{"name":"svy-progressbar.css", "version":"1", "url":"canvascomponents/progressbar/progressbar.css", "mimetype":"text/css"}, {"name":"jquery-ui.custom.js", "version":"1.9.2", "url":"http://preview.jumpstartthemes.com/canvas-admin/js/libs/jquery-ui-1.9.2.custom.min.js", "mimetype":"text/javascript"}],
	"model":
	{
		"label": {"type":"string"},
	    "progressbarText":{"type":"string"},
	    "styleClass" : { "type" :"styleclass"},
	    "value": {"type":"float", "default":0}
	}
}
