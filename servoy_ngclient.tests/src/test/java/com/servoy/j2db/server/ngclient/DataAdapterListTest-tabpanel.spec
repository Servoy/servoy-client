{
	"name": "tabpanel",
	"displayName": "TabPanel",
	"definition": "tabpanel.js",
	"libraries": [],
    "model": {
        "tabs": { "type": "tab[]", "pushToServer": "deep", "droppable": true, "tags": { "allowaccess": "visible", "wizard": "autoshow"}}
    },

    "types": {
        "tab": {
            "_id": { "type": "string", "tags": { "scope": "private" }, "pushToServer": "reject" },
            "containedForm": {"type":"form", "tags": {"useAsCaptionInDeveloper" : true, "wizard": {"order": "1", "wizardRelated": "relationName"}}},
            "relationName": {"type":"relation"}
        }
    }
} 
