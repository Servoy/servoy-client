{
	"name": "dialogs",
	"displayName": "Servoy Dialogs plugin",
	"version": 1,
	"definition": "servoyservices/dialogs/dialogs.js",
	"libraries":  [{"name":"bootbox", "version":"4.3.0", "url":"servoyservices/dialogs/bootbox.js", "mimetype":"text/javascript"},{"name":"dialogscss", "version":"1", "url":"servoyservices/dialogs/dialogs.css", "mimetype":"text/css"}],
	"model":
	{
	},
	"api":
	{
	 	 "showErrorDialog": {
	            "returns": "string",
	            "parameters":[
	            				{
						            "name":"dialogTitle",
						            "type":"string"
					            },
					            {
						            "name":"dialogMessage",
						            "type":"string"
						        },
					            {
						            "name":"buttonsText",
						            "type":"object",
						            "optional":"true"
						        }
	            			 ]
	        },
	     "showInfoDialog": {
	            "returns": "string",
	            "parameters":[
	            				{
						            "name":"dialogTitle",
						            "type":"string"
					            },
					            {
						            "name":"dialogMessage",
						            "type":"string"
						        },
					            {
						            "name":"buttonsText",
						            "type":"object",
						            "optional":"true"
						        }
	            			 ]
	        },
	      "showInputDialog": {
	            "returns": "string",
	            "parameters":[
	            				{
						            "name":"dialogTitle",
						            "type":"string",
						            "optional":"true"
					            },
					            {
						            "name":"dialogMessage",
						            "type":"string",
						            "optional":"true"
						        },
					            {
						            "name":"initialValue",
						            "type":"string",
						            "optional":"true"
						        }
	            			 ]
	        },
	       "showQuestionDialog": {
	            "returns": "string",
	            "parameters":[
	            				{
						            "name":"dialogTitle",
						            "type":"string"
					            },
					            {
						            "name":"dialogMessage",
						            "type":"string"
						        },
					            {
						            "name":"buttonsText",
						            "type":"object",
						            "optional":"true"
						        }
	            			 ]
	        },
	        "showSelectDialog": {
	            "returns": "string",
	            "parameters":[
	            				{
						            "name":"dialogTitle",
						            "type":"string"
					            },
					            {
						            "name":"dialogMessage",
						            "type":"string"
						        },
					            {
						            "name":"options",
						            "type":"object"
						        }
	            			 ]
	        },
	        "showWarningDialog": {
	            "returns": "string",
	            "parameters":[
	            				{
						            "name":"dialogTitle",
						            "type":"string"
					            },
					            {
						            "name":"dialogMessage",
						            "type":"string"
						        },
					            {
						            "name":"buttonsText",
						            "type":"object",
						            "optional":"true"
						        }
	            			 ]
	        }      
	}
}
