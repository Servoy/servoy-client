{
	"name": "svy-navigator",
	"displayName": "Servoy default navigator ",
	"definition": "servoydefault/navigator/navigator.js",
	"libraries": [{"name":"navigator.css", "version":"1", "url":"servoydefault/navigator/css/navigator.css", "mimetype":"text/css"}, {"name":"slider.js", "version":"1", "url":"servoycomponents/slider/slider.js", "mimetype":"text/javascript"}, {"name":"slider.css", "version":"1", "url":"servoycomponents/slider/css/slider.css", "mimetype":"text/css"}],
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