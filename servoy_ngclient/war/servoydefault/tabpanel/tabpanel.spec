{

	"name": "servoydefault-tabpanel",
	"displayName": "Tab Panel",
	"version": 1,
	"icon": "servoydefault/tabpanel/tab.png",
	"definition": "servoydefault/tabpanel/tabpanel.js",
	"serverscript": "servoydefault/tabpanel/tabpanel_server.js",
	"libraries": [{"name":"tabpanelcustom", "version":"1.0.1", "url":"servoydefault/tabpanel/tabpanelcustom.css", "mimetype":"text/css"}],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["onChangeMethodID","onTabChangeMethodID"] }, 
	        "fontType" : {"type":"font","stringformat":true}, 
	        "foreground" : "color", 
	        "horizontalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "selectedTabColor" : "color", 
	        "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :[]}, 
	        "tabIndex" : { "type": "object", "pushToServer": "shallow" }, 
	        "tabOrientation" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"default" :0}, {"TOP":1}, {"HIDE":-1}]}, 
	        "tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }}, 
	        "tabs" : {"type":"tab[]", "pushToServer": "shallow", "droppable":true}, 
	        "transparent" : "boolean", 
	        "visible" : "visible",
			"activeTabIndex": { "type": "int", "default": 0, "tags": { "scope": "private" }, "pushToServer": "shallow" }
	},

	"handlers": {
		"onChangeMethodID": {
			"parameters": [{
				"name": "previousIndex",
				"type": "int"
			}, {
				"name": "event",
				"type": "JSEvent"
			}]
		}
	},

	"api": {
		"addTab": {
			"returns": "boolean",
			"parameters": [{
				"name": "form/formname",
				"type": "object []"
			}, {
				"name": "name",
				"type": "object",
				"optional": true
			}, {
				"name": "tabText",
				"type": "object",
				"optional": true
			}, {
				"name": "tooltip",
				"type": "object",
				"optional": true
			}, {
				"name": "iconURL",
				"type": "object",
				"optional": true
			}, {
				"name": "fg",
				"type": "object",
				"optional": true
			}, {
				"name": "bg",
				"type": "object",
				"optional": true
			}, {
				"name": "relatedfoundset/relationname",
				"type": "object",
				"optional": true
			}, {
				"name": "index",
				"type": "object",
				"optional": true
			}]
		},
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
		"getMaxTabIndex": {
			"returns": "int"
		},
		"getMnemonicAt": {
			"returns": "string",
			"parameters": [{
				"name": "i",
				"type": "int"
			}]
		},
		"getSelectedTabFormName": {
			"returns": "string"
		},
		"getTabBGColorAt": {
			"returns": "string",
			"parameters": [{
				"name": "unnamed_0",
				"type": "int"
			}]
		},
		"getTabFGColorAt": {
			"returns": "string",
			"parameters": [{
				"name": "i",
				"type": "int"
			}]
		},
		"getTabFormNameAt": {
			"returns": "string",
			"parameters": [{
				"name": "i",
				"type": "int"
			}]
		},
		"getTabNameAt": {
			"returns": "string",
			"parameters": [{
				"name": "i",
				"type": "int"
			}]
		},
		"getTabRelationNameAt": {
			"returns": "string",
			"parameters": [{
				"name": "i",
				"type": "int"
			}]
		},
		"getTabTextAt": {
			"returns": "string",
			"parameters": [{
				"name": "i",
				"type": "int"
			}]
		},
		"getWidth": {
			"returns": "int"
		},
		"isTabEnabled": {
			"returns": "boolean",
			"parameters": [{
				"name": "unnamed_0",
				"type": "int"
			}]
		},
		"isTabEnabledAt": {
			"returns": "boolean",
			"parameters": [{
				"name": "i",
				"type": "int"
			}]
		},
		"removeAllTabs": {
			"returns": "boolean"
		},
		"removeTabAt": {
			"returns": "boolean",
			"parameters": [{
				"name": "index",
				"type": "int"
			}]
		},
		"setMnemonicAt": {
			"parameters": [{
				"name": "index",
				"type": "int"
			}, {
				"name": "text",
				"type": "string"
			}]
		},
		"setTabBGColorAt": {
			"parameters": [{
				"name": "unnamed_0",
				"type": "int"
			}, {
				"name": "unnamed_1",
				"type": "string"
			}]
		},
		"setTabEnabled": {
			"parameters": [{
				"name": "unnamed_0",
				"type": "int"
			}, {
				"name": "unnamed_1",
				"type": "boolean"
			}]
		},
		"setTabEnabledAt": {
			"parameters": [{
				"name": "i",
				"type": "int"
			}, {
				"name": "b",
				"type": "boolean"
			}]
		},
		"setTabFGColorAt": {
			"parameters": [{
				"name": "i",
				"type": "int"
			}, {
				"name": "s",
				"type": "string"
			}]
		},
		"setTabTextAt": {
			"parameters": [{
				"name": "index",
				"type": "int"
			}, {
				"name": "text",
				"type": "string"
			}]
		}
	},

	"types": {
		"tab": {
		    "_id": { "type": "string", "tags": { "scope": "private" }, "pushToServer": "reject" },
			"name": { "type": "string", "tags": { "useAsCaptionInDeveloper" : true, "captionPriority" : 1 } },
			"containsFormId": "form",
			"text": { "type": "tagstring", "tags": { "useAsCaptionInDeveloper" : true, "captionPriority" : 2 } },
			"relationName": "relation",
			"active": "boolean",
			"foreground": "color",
			"disabled": "boolean",
			"imageMediaID": "media",
			"mnemonic": "string",
			"toolTipText" : "tagstring", 
			"isActive": { "type": "boolean", "default": false, "tags": { "scope": "private" }, "pushToServer": "shallow" }
		}
	}

}