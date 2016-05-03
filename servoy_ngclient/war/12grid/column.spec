{
	"name": "column",
	"displayName": "Column",
	"version": 1,
	"icon": "12grid/column.png",
	"definition": "column.json",
	"contains": ["bootstrapcomponents.*", "12grid.row", "12grid.1column", "12grid.2columns", "12grid.3columns", "12grid.div", "12grid.labelfield", "12grid.responsivetable", "12grid.collapsible", "12grid.2screens","12grid.formreference"],
	"model": {
		"class" :{ "type" :"styleclass", "tags": { "scope" :"design" } , "values" :["col-xs-","col-sm-","col-md-","col-md-4","col-lg-","col-xs-offset-","col-sm-offset-","col-md-offset-","col-lg-offset-"]}
	}
}
