{
	"name": "svy-navigator",
	"displayName": "Servoy default navigator ",
	"definition": "servoydefault/navigator/navigator.js",
	"libraries": ["servoydefault/navigator/css/navigator.css", "servoycomponents/slider/slider.js", "servoycomponents/slider/css/slider.css"],
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