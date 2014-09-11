{
	"name": "svy-calendar",
	"displayName": "Calendar",
	"icon": "servoydefault/calendar/Calendar_C16.png",
	"definition": "servoydefault/calendar/calendar.js",
	"libraries": [{"name":"moment.js", "version":"2.6.0", "url": "servoydefault/calendar/bootstrap-datetimepicker/js/moment.min.js", "mimetype":"text/javascript"},{"name":"bootstrap-datetimepicker.js", "version":"3.0.0", "url":"servoydefault/calendar/bootstrap-datetimepicker/js/bootstrap-datetimepicker.min.js", "mimetype":"text/javascript"},{"name":"bootstrap-datetimepicker.css", "version":"3.0.0", "url":"servoydefault/calendar/bootstrap-datetimepicker/css/bootstrap-datetimepicker.min.css", "mimetype":"text/css"}],
	"model":
	{
	        "background" : "color", 
	        "borderType" : "border", 
	        "dataProviderID" : { "type":"dataprovider", "scope" :"design", "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}}, 
	        "editable" : {"type":"boolean", "default":true}, 
	        "enabled" : {"type":"boolean", "default":true}, 
	        "fontType" : "font", 
	        "foreground" : "color", 
	        "format" : {"for":"dataProviderID" , "type" :"format"}, 
	        "horizontalAlignment" : {"type" :"int", "scope" :"design", "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
	        "location" : "point", 
	        "margin" : {"type" :"insets", "scope" :"design"}, 
	        "placeholderText" : "tagstring", 
	        "selectOnEnter" : {"type" :"boolean", "scope" :"design"}, 
	        "size" : {"type" :"dimension",  "default" : {"width":140, "height":20}}, 
	        "styleClass" : { "type" :"styleclass", "scope" :"design", "values" :["form-control", "input-sm", "svy-padding-xs", "svy-line-height-normal"]}, 
	        "tabSeq" : {"type" :"tabseq", "scope" :"design"}, 
	        "text" : "tagstring", 
	        "toolTipText" : "tagstring", 
	        "transparent" : "boolean", 
	        "visible" : {"type":"boolean", "default":true} 
	},
	"handlers":
	{
	        "onActionMethodID" : "function", 
	        "onDataChangeMethodID" : "function", 
	        "onFocusGainedMethodID" : "function", 
	        "onFocusLostMethodID" : "function", 
	        "onRenderMethodID" : "function", 
	        "onRightClickMethodID" : "function" 
	},
	"api":
	{
	        "requestFocus": {
				"parameters":[
								{                                                                 
 								"name":"mustExecuteOnFocusGainedMethod",
								"type":"boolean",
			            		"optional":"true"
			            		}             
							 ]
	        }
	}
	 
}