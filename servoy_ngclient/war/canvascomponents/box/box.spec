{
	"name": "canvascomponents-box",
	"displayName": "Box",
	"definition": "canvascomponents/box/box.js",
	"libraries":  [{"name":"font-awesome.css", "version":"4.2.0", "url":"//netdna.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.css", "mimetype":"text/css"}],
	"model":
	{
			"bgstyle" : { "type" :"styleclass"}, 
	        "bgimagestyle" : { "type" :"styleclass"}, 
	        "nextstyle" : { "type" :"styleclass"}, 
	        "title" : { "type":"tagstring" },
	        "value" : { "type":"dataprovider", "scope" :"design"}
	},
	"handlers":
	{
	    "onClick" : "function" 
	}
}