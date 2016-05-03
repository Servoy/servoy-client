{
	"name": "container",
	"displayName": "Container",
	"version": 1,
	"icon": "12grid/container.gif",
	"designStyleClass" : "rowDesign",
	"definition": "container.json",
	"contains": ["12grid.row","12grid.formreference"],
	"topContainer": true,
	"model": {
		"class" :{ "type" :"styleclass", "tags": { "scope" :"design" } , "values" :["container","container-fluid"]}
	}
}