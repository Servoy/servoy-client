{
	"name": "servoycore-listformcomponent",
	"displayName": "List FormComponent Container",
	"categoryName": "Form Containers",
	"version": 1,
	"icon": "servoycore/listformcomponent/listformcomponent.png",
	"definition": "servoycore/listformcomponent/listformcomponent.js", 
	"libraries": [{ "name": "svy-listformcomponent-css", "version": "1.0", "url": "servoycore/listformcomponent/listformcomponent.css", "mimetype": "text/css" }],
	"keywords": [],	
	"model":
	{
		"foundset" : {"type": "foundset", "default" : {"foundsetSelector":""}},
		"containedForm": {"type":"formcomponent", "forFoundset":"foundset"},
		"pageLayout" : {"type" : "string" , "values" : ["cardview","listview"] , "initialValue" : "cardview" },
		"responsivePageSize": { "type": "int", "tags": {"doc" : "This property sets the number of cells this component will generate when the this component is placed in a responsive form, ignored in absolute"}},
		"responsiveRowHeight": { "type": "int", "default": 300, "tags": {"doc" : "This property sets the height of a row for the scrolling LFC if the containedForm is a responisive form, else it is ignored."}},
		"styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "default": "svy-listformcomponent" },
		"rowStyleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }},
		"rowStyleClassDataprovider": { "type": "dataprovider", "forFoundset": "foundset" },
		"paginationStyleClass" : { "type" :"styleclass", "tags": { "scope" :"design" } },
		"readOnly" : { "type": "protected", "for":["readOnly"]},
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
		}
	}
}