{
	"name": "servoydefault-navigator",
	"displayName": "Servoy default navigator ",
	"version": 1,
	"definition": "servoydefault/navigator/navigator.js",
	"libraries": [{"name":"navigator.css", "version":"1", "url":"servoydefault/navigator/css/navigator.css", "mimetype":"text/css"}, {"name":"slider.js", "version":"1", "url":"servoydefault/slider/slider.js", "mimetype":"text/javascript"}],
	"model":
	{
	    "currentIndex": "long",
	    "maxIndex": "long"
	},
	"handlers":
	{
	    "setSelectedIndex": "function"
	}
}