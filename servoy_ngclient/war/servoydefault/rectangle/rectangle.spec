{
	"name": "servoydefault-rectangle",
	"displayName": "Rectangle",
	"version": 1,
	"icon": "servoydefault/rectangle/rectangle.png",
	"definition": "servoydefault/rectangle/rectangle.js",
	"libraries": [],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "containsFormID" : { "type": "form", "tags": { "serveronly": true }}, 
	        "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["dataProviderID","onActionMethodID","onDataChangeMethodID","onFocusGainedMethodID","onFocusLostMethodID","onRightClickMethodID"] }, 
	        "foreground" : "color", 
	        "lineSize" : "int", 
	        "roundedRadius" : "int", 
	        "shapeType" : "int", 
	        "size" : "dimension", 
	        "transparent" : "boolean", 
	        "visible" : "visible",
	        "designsize": { "type": "dimension", "tags": { "serveronly": true }, "default": {"width": 200, "height": 100} } 
	},
	"handlers":
	{
	},
	"api":
	{
	        "getFormName": {
	            "returns": "string"
	        },
	        "getHeight": {
	            "returns": "int"
	        },
	        "getLocationX": {
	            "returns": "int"
	        },
	        "getLocationY": {
	            "returns": "int"
	        },
	        "getWidth": {
	            "returns": "int"
	        }
	}
	 
}