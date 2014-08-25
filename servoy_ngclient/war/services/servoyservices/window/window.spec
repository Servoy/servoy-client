{
	"name": "window",
	"displayName": "Servoy Window plugin",
	"definition": "services/servoyservices/window/window.js",
	"serverscript": "services/servoyservices/window/window_server.js",
	"libraries": ["services/servoyservices/window/shortcut.js","services/servoyservices/window/yahoo-dom-event.js","services/servoyservices/window/container_core-min.js","services/servoyservices/window/fonts-min.css","services/servoyservices/window/menu-min.js","services/servoyservices/window/menu.css","services/servoyservices/window/servoy-menu.css"],
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
		            				"width":"int",
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
