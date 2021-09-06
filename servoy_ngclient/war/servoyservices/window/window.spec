{
	"name": "window",
	"displayName": "Servoy Window plugin",
	"version": 1,
	"definition": "servoyservices/window/window.js",
	"serverscript": "servoyservices/window/window_server.js",
	"ng2Config": {
       "packageName": "@servoy/window",
       "moduleName": "WindowServiceModule",
       "serviceName": "WindowPluginService",
       "entryPoint": "projects/window",
       "dependencies": {
            "csslibrary" : ["~@servoy/window/servoy-menu.css"]
        }
    },
	"libraries": [{"name":"window/shortcut.js", "version":"1", "url":"servoyservices/window/shortcut.js", "mimetype":"text/javascript"},{"name":"yahoo-dom-event.js", "version":"2.9.0", "url":"servoyservices/window/yahoo-dom-event.js", "mimetype":"text/javascript"},{"name":"window/container_core.js", "version":"2.9.0", "url":"servoyservices/window/container_core-min.js", "mimetype":"text/javascript"},{"name":"menu.js", "version":"2.9.0", "url":"servoyservices/window/menu-min.js", "mimetype":"text/javascript"},{"name":"menu.css", "version":"2.9.0", "url":"servoyservices/window/menu.css", "mimetype":"text/css"},{"name":"servoy-menu.css", "version":"1", "url":"servoyservices/window/servoy-menu.css", "mimetype":"text/css"}],
	"model":
	{
	 	"shortcuts" : { "type": "shortcut[]", "tags": { "scope" :"private" }},
	 	"popupform": {"type": "popupform", "tags": { "scope" :"private" }},
	 	"popupMenus" : {"type": "popup[]", "tags": { "scope" :"private" }},
	 	"popupMenuShowCommand" : {"type": "popupMenuShowCommand", "pushToServer": "shallow", "tags": { "scope" :"private" }} 
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
					            },
					            {
						            "name":"consumeEvent",
						            "type":"boolean",
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
		            				"type":"runtimecomponent"
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
		            				
	            				},
	            				{
		            				"name":"x",
		            				"type":"int",
		            				"optional":"true"
	            				},
	            				{
		            				"name":"y",
		            				"type":"int",
		            				"optional":"true"
		            				
	            				},
	            				{
		            				"name":"showBackdrop",
		            				"type":"boolean",
		            				"optional":"true"
		            				
	            				},
	            				{
		            				"name":"doNotCloseOnClickOutside",
		            				"type":"boolean",
		            				"optional":"true"
		            				
	            				},
	            				{
		            				"name":"onClose",
		            				"type":"function",
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
	     "createFormPopup": {
	     	 "parameters":[
	            {
		            "name":"component",
		            "type":"runtimecomponent"
	            },
	            {
		            "name":"form",
		            "type":"form"
	            }
	           ],
	          "returns": "FormPopup"
	      },
	     "createPopupMenu": {
	            "returns": "popup"
	        },
	     "cleanup": {
	     	}
	},
	"internalApi":
    {
        "clearPopupForm" :{
        },
        "cancelFormPopupInternal": {
        	"parameters":[
	            {
		            "name":"disableClearPopupFormCallToServer",
		            "type":"boolean"
	            }
	        ]
	    },
	     "executeMenuItem" :{
	           "parameters":[
                 {
                    "name":"menuItemId",
                    "type":"string"
                 },
                 {
                    "name":"itemIndex",
                    "type":"int"
                 },
                 {
                    "name":"parentItemIndex",
                    "type":"int"
                 },
                 {
                    "name":"isSelected",
                    "type":"boolean"
                 },
                 {
                    "name":"parentMenuText",
                    "type":"string"
                 },
                 {
                    "name":"menuText",
                    "type":"string"
                 }
            ]
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
	  		"component": "runtimecomponent",
	  		"form": "form",
	  		"width": "int",
	  		"height": "int",
	  		"x": "int",
	  		"y": "int",
			"showBackdrop": "boolean",
			"doNotCloseOnClickOutside": "boolean",
			"onClose": "function" 
	  	}
	  },
	  "FormPopup": {
	  	"model": {
	  	    "component": "runtimecomponent",
	  	    "dataprovider" : "string",
	  	    "scope": "object",
	  		"width": "int",
	  		"height": "int",
	  		"x": "int",
	  		"y": "int",
			"showBackdrop": "boolean",
			"doNotCloseOnClickOutside": "boolean" 
	  	}
	  },
	  "popupMenuShowCommand":{
		  "model": {
			"popupName": "string",
			"elementId": "string",
			"x": "int",
			"y": "int",
			"checkAbove": "boolean"
		}
	  },
	  "menuitem": {
		"model": {
		    "id":"string",
		 	"text": "string",
		  	"callback": "function",
		  	"name": "string",
		  	"align": "int",
		  	"enabled": { "type": "protected", "blockingOn": false, "default": true },
		  	"visible": "visible",
		  	"icon": "media",
		  	"fa_icon": "string",
		  	"mnemonic": "string",
		  	"backgroundColor": "string",
		  	"foregroundColor": "string",
		  	"selected": "boolean",
		  	"accelarator": "string",
		  	"methodArguments": "object[]",
		  	"cssClass": "string",
		  	"items": "menuitem[]"
		}
	  },
	  "popup": {
	  	"model": {
	  		"name": "string",
	  		"cssClass" : "string",
	  		"items": "menuitem[]"
	  	}
	  }
	}
}
