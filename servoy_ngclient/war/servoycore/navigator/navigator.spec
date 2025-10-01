{
	"name": "servoycore-navigator",
	"displayName": "Servoy default navigator ",
	"version": 1,
	"definition": "servoycore/navigator/navigator.js",
	"doc": "servoycore/navigator/navigator_doc.js",
	"libraries": [{"name":"navigator.css", "version":"1", "url":"servoycore/navigator/css/navigator.css", "mimetype":"text/css"}, {"name":"slider.js", "version":"1", "url":"servoycore/slider/slider.js", "mimetype":"text/javascript"}],
	"model":
	{
	    "currentIndex": "long",
	    "maxIndex": "long",
	    "minIndex": "long",
	    "hasMore": "boolean"
	},
	"handlers":
	{
	    "setSelectedIndex": "function"
	}
}