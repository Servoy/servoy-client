{
	"name": "column",
	"displayName": "Column",
	"version": 1,
	"definition": "column.json",
	"contains": ["component","row"],
	"parents": ["row"],
	"model": {
		"class" :{ "type" :"styleclass", "tags": { "scope" :"design" } , "values" :[".col-xs-",".col-sm-",".col-md-",".col-lg-",".col-xs-offset-",".col-sm-offset-",".col-md-offset-",".col-lg-offset-"]}
	}
}