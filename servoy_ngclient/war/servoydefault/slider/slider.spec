{
	"name": "servoydefault-slider",
	"displayName": "Slider",
	"version": 1,
	"icon": "servoydefault/slider/SLIDER16.png",
	"definition": "servoydefault/slider/slider.js", 
	"libraries": [{"name":"jquery-ui.custom", "version":"1.9.2", "url":"servoydefault/slider/css/jquery-ui-1.9.2.custom.css", "mimetype":"text/css"},
				  {"name":"jquery-ui.slider", "version":"1.10.4", "url":"servoydefault/slider/js/jquery-ui.slider.min.js", "mimetype":"text/javascript"}, 
	 			  {"name":"jquery-ui.slider", "version":"1.10.4", "url":"servoydefault/slider/css/jquery-ui.slider.min.css", "mimetype":"text/css"},
				  {"name":"ui-slider", "version":"1", "url":"servoydefault/slider/ui-slider/slider.js", "mimetype":"text/javascript"}],	
	
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