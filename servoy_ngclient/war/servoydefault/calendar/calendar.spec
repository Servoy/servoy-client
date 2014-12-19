{
	"name": "servoydefault-calendar",
	"displayName": "Calendar",
	"version": 1,
	"icon": "servoydefault/calendar/Calendar_C16.png",
	"definition": "servoydefault/calendar/calendar.js",
	"libraries": [{"name":"moment", "version":"2.6.0", "url": "servoydefault/calendar/bootstrap-datetimepicker/js/moment.min.js", "mimetype":"text/javascript"},{"name":"moment-jdateformatparser", "version":"0.1.1", "url":"servoydefault/calendar/bootstrap-datetimepicker/js/moment-jdateformatparser.js", "mimetype":"text/javascript"},{"name":"bootstrap-datetimepicker", "version":"4.0.0b", "url":"servoydefault/calendar/bootstrap-datetimepicker/js/bootstrap-datetimepicker.js", "mimetype":"text/javascript"},{"name":"bootstrap-datetimepicker", "version":"4.0.0b", "url":"servoydefault/calendar/bootstrap-datetimepicker/css/bootstrap-datetimepicker.css", "mimetype":"text/css"}],
	"model":
	{
	        "background" : "color", 
	        "borderType" : {"type":"border","stringformat":true}, 
	        "dataProviderID" : { "type":"dataprovider", "scope" :"design", "ondatachange": { "onchange":"onDataChangeMethodID", "callback":"onDataChangeCallback"}}, 
	        "editable" : {"type":"boolean", "default":true}, 
	        "enabled" : { "type": "protected", "blockingOn": false, "default": true },
	        "fontType" : {"type":"font","stringformat":true}, 
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
	        "visible" : "visible"
	},
	"handlers":
	{
	        "onActionMethodID" : "function", 
	        "onDataChangeMethodID" : "function", 
	        "onFocusGainedMethodID" : "function", 
	        "onFocusLostMethodID" : "function", 
	        "onRightClickMethodID" : "function" 
	},
	"api":
	{
	        "requestFocus": {
				"parameters":[
								{                                                                 
 								"name":"mustExecuteOnFocusGainedMethod",
								"type":"boolean",
			            		"optional":true
			            		}             
							 ]
	        }
	}
	 
}