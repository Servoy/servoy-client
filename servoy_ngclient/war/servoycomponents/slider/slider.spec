{
	"name": "slider",
	"displayName": "Slider",
	"definition": "servoycomponents/slider/slider.js",
	"libraries": [{"name":"ui-slider/slider.js", "version":"1", "url":"servoycomponents/slider/ui-slider/slider.js", "mimetype":"text/javascript"}, {"name":"jquery-ui.slider.js", "version":"1.10.4", "url":"servoycomponents/slider/js/jquery-ui.slider.min.js", "mimetype":"text/javascript"}, {"name":"jquery-ui.slider.css", "version":"1.10.4", "url":"servoycomponents/slider/css/jquery-ui.slider.min.css", "mimetype":"text/css"}, {"name":"slider/css/slider.css", "version":"1", "url":"servoycomponents/slider/css/slider.css", "mimetype":"text/css"}],
	"model":
	{
		"animate": "string",
	    "dataProviderID": { "type":"dataprovider", "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}}, 
	    "enabled": {"type":"boolean", "default":true}, 
	    "max": {"type":"long", "default":100},
	    "min": {"type":"long", "default":0},
	    "orientation": {"type":"string", "values":["horizontal", "vertical"], "default":"horizontal"},
	    "range": {"type":"string", "values":["min", "max"], "default":"min"},
	    "step": {"type":"long", "default":1},
	    "visible": {"type":"boolean", "default":true} 
	},
	"handlers":
	{
	    "onChangeMethodID": "function",
	    "onCreateMethodID": "function",
	    "onSlideMethodID": "function",
		"onStartMethodID": "function",
		"onStopMethodID": "function"
	}
}