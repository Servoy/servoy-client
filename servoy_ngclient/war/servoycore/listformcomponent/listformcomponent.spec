{
	"name": "servoycore-listformcomponent",
	"displayName": "List Form Component Container",
	"categoryName": "Form Containers",
	"version": 1,
	"icon": "servoycore/listformcomponent/listformcomponent.png",
	"definition": "servoycore/listformcomponent/listformcomponent.js", 
	"doc": "servoycore/listformcomponent/listformcomponent_doc.js", 
	"libraries": [{ "name": "svy-listformcomponent-css", "version": "1.0", "url": "servoycore/listformcomponent/listformcomponent.css", "mimetype": "text/css" }],
	"keywords": [],	
	"model":
	{
		"foundset" : {"type": "foundset", "default" : {"foundsetSelector":""}},
		"containedForm": {"type":"formcomponent", "forFoundset":"foundset", "tags": { "wizard": "autoshow"}},
		"containedFormMargin": {"type":"insets", "tags": {"doc" : "Margin added to the containedForm. Use this property instead of adding margin via CSS for a correct layout of the contained forms."}},
		"pageLayout" : {"type" : "string" , "values" : ["cardview","listview"] , "initialValue" : "cardview" },
		"responsivePageSize": { "type": "int", "tags": {"doc" : "This property in only used when the component is placed in a responsive form; it is ignored in absolute; when used in paging mode (client property UICONSTANTS.LISTFORMCOMPONENT_PAGING_MODE = true) it sets the number of records displayed in a single page; when used in scrolling mode (which is the default mode) it is only used when pageLayout is set to cardiew, and it sets the maximum records displayed in a single row of the listformcomponent."}},
		"responsiveHeight": { "type": "int", "default": 300, "tags": {"doc" : "This property sets the height of the listformcomponent when using scrolling mode in a responisive form. Adding a listformcomponent in a flex-content layout and setting responsiveHeight property to 0, let the listformcomponent grow up to 100% height of parent element (see more on flex-layout here: https://github.com/Servoy/12grid/wiki/Flexbox-Layout ). Used with other containers than flex-content layout in order to grow the listformcomponent to 100% height, the parent element must have a known height. When responsiveHeight is set to -1, the LFC will auto-size it's height to the number of rows displayed - in this case there is no vertical scrollbar and all rows are rendered"}},
		"styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "default": "svy-listformcomponent" },
		"rowStyleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }},
		"rowStyleClassDataprovider": { "type": "dataprovider", "forFoundset": "foundset" },
		"rowEditableDataprovider": { "type": "dataprovider", "forFoundset": "foundset" },
		"rowEnableDataprovider": { "type": "dataprovider", "forFoundset": "foundset" },
		"paginationStyleClass" : { "type" :"styleclass", "tags": { "scope" :"design" } },
		"readOnly" : { "type": "protected", "for":["readOnly"], "tags": {"scope":"private"} },
		"editable" : { "type": "protected", "blockingOn": false, "default": true, "for":["editable"]},
		"selectionClass": { "type": "styleclass", "tags": { "scope" :"design", "doc": "In case <b>rowStyleClassDataprovider</b> or <b>rowStyleClass</b> are used, make sure that the selection styleclass definition is last in the solution stylesheet, to avoid overwriting it.<br/><i>.listitem-info</i> {<br/>&nbsp;&nbsp;&nbsp;<i>background-color: blue;</i><br/>}<br/><i>.listitem-selected</i> {<br/>&nbsp;&nbsp;&nbsp;<i>background-color: orange;</i><br/>}" }},
		"tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }},
		"visible" : "visible"
	},
	"handlers" : {
		"onSelectionChanged": {
			"description": "Called after the foundset selection changed",
			"parameters": [{
				"name": "event",
				"type": "JSEvent"
			}]
		},
		"onListItemClick": {
			"description": "Called when a list item is clicked",
			"parameters": [{
				"name": "record",
				"type": "record",
				"tags": { "skipCallIfNotSelected": true }
			},{
				"name": "event",
				"type": "JSEvent"
			}]
		}
	}
}