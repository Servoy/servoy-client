{
	"name": "dialogs",
	"displayName": "Servoy Dialogs plugin",
	"version": 1,
	"definition": "servoydefaultservices/dialogs/dialogs.js",
	"libraries": 
	[
		
		{
			"name": "bootstrap",
			"version": "3.4.1",
			"url": "servoydefaultservices/dialogs/bootstrap.min.js",
			"mimetype": "text/javascript"
		},
		{
			"name": "bootbox",
			"version": "4.3.0",
			"url": "servoydefaultservices/dialogs/bootbox.js",
			"mimetype": "text/javascript"
		},

		{
			"name": "dialogscss",
			"version": "1",
			"url": "servoydefaultservices/dialogs/dialogs.css",
			"mimetype": "text/css"
		}
	],

	"model": 
	{
		
	},

	"api": 
	{
		"showErrorDialog": 
		{
			"blockEventProcessing": false,
			"returns": "string",
			"parameters": 
			[
				{
					"name": "dialogTitle",
					"type": "string"
				},

				{
					"name": "dialogMessage",
					"type": "string"
				},

				{
					"name": "buttonsText",
					"type": "string...",
					"optional": "true"
				}
			]
		},

		"showInfoDialog": 
		{
			"blockEventProcessing": false,
			"returns": "string",
			"parameters": 
			[
				{
					"name": "dialogTitle",
					"type": "string"
				},

				{
					"name": "dialogMessage",
					"type": "string"
				},

				{
					"name": "buttonsText",
					"type": "string...",
					"optional": "true"
				}
			]
		},

		"showInputDialog": 
		{
			"blockEventProcessing": false,
			"returns": "string",
			"parameters": 
			[
				{
					"name": "dialogTitle",
					"type": "string",
					"optional": "true"
				},

				{
					"name": "dialogMessage",
					"type": "string",
					"optional": "true"
				},

				{
					"name": "initialValue",
					"type": "string",
					"optional": "true"
				}
			]
		},

		"showQuestionDialog": 
		{
			"blockEventProcessing": false,
			"returns": "string",
			"parameters": 
			[
				{
					"name": "dialogTitle",
					"type": "string"
				},

				{
					"name": "dialogMessage",
					"type": "string"
				},

				{
					"name": "buttonsText",
					"type": "string...",
					"optional": "true"
				}
			]
		},

		"showSelectDialog": 
		{
			"blockEventProcessing": false,
			"returns": "string",
			"parameters": 
			[
				{
					"name": "dialogTitle",
					"type": "string"
				},

				{
					"name": "dialogMessage",
					"type": "string"
				},

				{
					"name": "options",
					"type": "string..."
				}
			]
		},

		"showWarningDialog": 
		{
			"blockEventProcessing": false,
			"returns": "string",
			"parameters": 
			[
				{
					"name": "dialogTitle",
					"type": "string"
				},

				{
					"name": "dialogMessage",
					"type": "string"
				},

				{
					"name": "buttonsText",
					"type": "string...",
					"optional": "true"
				}
			]
		}
	}
}