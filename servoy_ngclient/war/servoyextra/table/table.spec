{
	"name": "servoyextra-table",
	"displayName": "Table",
	"version": 1,
	"icon": "servoycore/portal/portal.gif",
	"definition": "servoyextra/table/table.js",
	"libraries": [{"name":"servoyextra-table-css", "version":"1.0", "url":"servoyextra/table/table.css", "mimetype":"text/css"},
				{"name":"colResizable", "version":"1.6", "url":"servoyextra/table/js/colResizable-1.6.min.js", "mimetype":"text/javascript"}],
	"model":
	{
		"columns":  { "type":"column[]", "droppable": true, "pushToServer": "shallow", "elementConfig" : {"pushToServer": "shallow"}},
		"currentPage":  { "type":"int", "default" : 1, "tags": { "scope": "runtime" } },
		"foundset": { "type": "foundset", "pushToServer": "allow" },
		"pageSize" : { "type":"int", "default" : 20},
		"styleClass" : { "type": "styleclass", "tags": { "scope": "design" }, "default": "table", "values": ["table", "table-striped", "table-bordered", "table-hover", "table-condensed"] },
		"selectionClass" : { "type": "styleclass", "default": "table-servoyextra-selected "},
		"tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }},
	 	"visible" : "visible"
	},
	"types":
	{
		"column":
		{
			"dataprovider": {	"type": "dataprovider",	"forFoundset": "foundset" },
			"format" : {"for":["valuelist","dataprovider"] , "type" :"format"},
			"headerStyleClass" : { "type" :"styleclass"}, 
			"headerText": {"type" :"string", "initialValue" : "header", "tags": { "showInOutlineView" :true }},
			"styleClass" : { "type" :"styleclass"},
			"styleClassDataprovider" : { "type": "dataprovider",	"forFoundset": "foundset"},
			"valuelist" : { "type" : "valuelist", "tags": { "scope" :"design" }, "for": "dataprovider"},
			"width" : {"type" : "int"}
		}
	},
	"handlers":
	{
		"onCellClick" : {
				"description": "Called when the mouse is clicked on a row/cell (foundset and column indexes are given) or\nwhen the ENTER key is used then only the selected foundset index is given\nUse the record to exactly match where the user clicked on",
	        	"parameters":[
					{
						"name":"foundsetindex",
						"type":"int"
					},
					{
						"name":"columnindex",
						"type":"int",
						"optional":true
					},
					{
						"name":"record",
						"type":"record",
						"optional":true
					}
				]
		},
		"onCellRightClick" : {
			"description": "Called when the right mouse button is clicked on a row/cell (foundset and column indexes are given) or\nwhen the ENTER key is used then only the selected foundset index is given\nUse the record to exactly match where the user clicked on",
        	"parameters":[
				{
					"name":"foundsetindex",
					"type":"int"
				},
				{
					"name":"columnindex",
					"type":"int",
					"optional":true
				},
				{
					"name":"record",
					"type":"record",
					"optional":true
				}
			]
		},
		"onHeaderClick" : {
	        	"parameters":[
					{
						"name":"columnindex",
						"type":"int"
					}
				]
		}
	}
}
