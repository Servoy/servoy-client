{
	"name": "dialogs",
	"displayName": "Servoy Dialogs plugin",
	"version": 1,
	"definition": "servoyservices/dialogs/dialogs.js",
	"libraries": 
	[
		{
			"name": "bootbox",
			"version": "4.3.0",
			"url": "servoyservices/dialogs/bootbox.js",
			"mimetype": "text/javascript"
		},

		{
			"name": "dialogscss",
			"version": "1",
			"url": "servoyservices/dialogs/dialogs.css",
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
					"type": "object",
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
					"type": "object",
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
					"type": "object",
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
					"type": "object"
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
					"type": "object",
					"optional": "true"
				}
			]
		}
	}
}