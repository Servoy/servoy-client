{
	"name": "window",
	"displayName": "Servoy Window plugin",
	"definition": "servoyservices/window/window.js",
	"serverscript": "servoyservices/window/window_server.js",
	"libraries": [{"name":"window/shortcut.js", "version":"1", "url":"servoyservices/window/shortcut.js", "mimetype":"text/javascript"},{"name":"yahoo-dom-event.js", "version":"2.9.0", "url":"servoyservices/window/yahoo-dom-event.js", "mimetype":"text/javascript"},{"name":"window/container_core.js", "version":"2.9.0", "url":"servoyservices/window/container_core-min.js", "mimetype":"text/javascript"},{"name":"menu.js", "version":"2.9.0", "url":"servoyservices/window/menu-min.js", "mimetype":"text/javascript"},{"name":"menu.css", "version":"2.9.0", "url":"servoyservices/window/menu.css", "mimetype":"text/css"},{"name":"servoy-menu.css", "version":"1", "url":"servoyservices/window/servoy-menu.css", "mimetype":"text/css"}],
	"model":
	{
	 	"shortcuts" : "shortcut[]",
	 	"popupform": "popupform",
	 	"popupMenus" : "popup[]",
	 	"popupMenuShowCommand" : "popupMenuShowCommand" 
	},
	"api":
	{
	 	 "createShortcut": {
	            "returns": "boolean",
	            "parameters":[
	            				{
						            "name":"shortcut",
						            "type":"string"
					            },
					            {
						            "name":"callback",
						            "type":"function"
						        },
					            {
						            "name":"contextFilter",
						            "type":"string",
						            "optional":"true"
						        },
					            {
						            "name":"arguments",
						            "type":"object []",
						            "optional":"true"
					            }
	            			 ]
	        },
	     "removeShortcut": {
	            "returns": "boolean",
	            "parameters":[
					            {
						            "name":"shortcut",
						            "type":"string"
					            },
					            {
						            "name":"contextFilter",
						            "type":"string",
						            "optional":"true"
					            }
	            			 ]
	        },
	     "showFormPopup": {
	            "parameters":[
	            				{
		            				"name":"component",
		            				"type":"component"
	            				},
	            				{
		            				"name":"form",
		            				"type":"form"
	            				},
	            				{
		            				"name":"scope",
		            				"type":"object"
	            				},
	            				{
		            				"name":"dataProviderID",
		            				"type":"string"
	            				},
	            				{
		            				"name":"width",
		            				"type":"int",
		            				"optional":"true"
	            				},
	            				{
		            				"name":"height",
		            				"type":"int",
		            				"optional":"true"
		            				
	            				}
	            			 ]
	        },
	     "closeFormPopup": {
	     		"parameters":[
	     						{
	     						"name":"retval",
	     						"type":"object"
	     						}
	     					 ]
	     	},
	     "cancelFormPopup": {
	     	},
	     "createPopupMenu": {
	            "returns": "popup"
	        }
	},
	"types": {
	  "shortcut": {
	  	"model": {
	  		"shortcut": "string",
	  		"callback": "function",
	  		"contextFilter": "string",
	  		"arguments": "object[]"
	  	}
	  },
	  "popupform": {
	  	"model": {
	  		"visible": "boolean",
	  		"componentLocation": "string",
	  		"form": "string",
	  		"scope": "string",
	  		"dataProviderID": "string",
	  		"width": "int",
	  		"height": "int"
	  	}
	  },
	  "popupMenuShowCommand":{
		  "model": {
			"popupName": "string",
			"elementId": "string",
			"x": "int",
			"y": "int"
		}
	  },
	  "menuitem": {
		"model": {
		 	"text": "string",
		  	"callback": "function",
		  	"name": "string",
		  	"align": "int",
		  	"enabled": "boolean",
		  	"visible": "boolean",
		  	"icon": "media",
		  	"mnemonic": "string",
		  	"backgroundColor": "string",
		  	"foregroundColor": "string",
		  	"selected": "boolean",
		  	"accelarator": "string",
		  	"args": "object[]",
		  	"cssClass": "string",
		  	"items": "menuitem[]"
		}
	  },
	  "popup": {
	  	"model": {
	  		"name": "string",
	  		"items": "menuitem[]"
	  	}
	  }
	}
}
