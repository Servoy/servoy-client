{
    "name": "servoydefault-htmlarea",
    "displayName": "Html Area",
    "version": 1,
    "icon": "servoydefault/htmlarea/html_area.png",
    "definition": "servoydefault/htmlarea/htmlarea.js",
    "libraries": [{"name":"tinymce", "version":"5.10.9", "url":"servoydefault/htmlarea/lib/tinymce/tinymce.min.js", "mimetype":"text/javascript", "group":false},{"name":"ui-tinymce", "version":"1", "url":"servoydefault/htmlarea/lib/ui-tinymce.js", "mimetype":"text/javascript"}],
    "ng2Config": {
       "assets": [{
                "glob" : "tinymce.min.js",
                "input" : "node_modules/tinymce",
                "output": "/tinymce/"
            },
            {
                "glob" : "plugins/*/plugin.min.js",
                "input" : "node_modules/tinymce",
                "output": "/tinymce/"
            },
            {
                "glob" : "themes/*/theme.min.js",
                "input" : "node_modules/tinymce",
                "output": "/tinymce/"
            },
            {
                "glob" : "models/*/model.min.js",
                "input" : "node_modules/tinymce",
                "output": "/tinymce/"
            },
            {
                "glob" : "skins/**",
                "input" : "node_modules/tinymce",
                "output": "/tinymce/"
            },
            {
                "glob" : "icons/*/icons.min.js",
                "input" : "node_modules/tinymce",
                "output": "/tinymce/"
            },
            {
                "glob" : "**/*",
                "input" : "node_modules/tinymce-i18n/langs",
                "output": "/tinymce/langs"
            },
            {
                "glob" : "**/*",
                "input" : "node_modules/tinymce-i18n/langs5",
                "output": "/tinymce/langs5"
            }
        ]
    },
    "model":
    {
            "background" : "color", 
            "borderType" : {"type":"border","stringformat":true}, 
            "dataProviderID" : { "type":"dataprovider", "pushToServer": "allow", "tags": { "wizard": true, "scope": "design" }, "ondatachange": { "onchange":"onDataChangeMethodID"}, "displayTagsPropertyName" : "displaysTags"}, 
            "displaysTags" : { "type" : "boolean", "tags": { "scope" : "design" } }, 
            "editable" : { "type": "protected", "blockingOn": false, "default": true,"for": ["dataProviderID","onDataChangeMethodID"] }, 
            "enabled" : { "type": "enabled", "blockingOn": false, "default": true, "for": ["dataProviderID","onActionMethodID","onDataChangeMethodID","onFocusGainedMethodID","onFocusLostMethodID","onRightClickMethodID"] }, 
            "findmode" : { "type":"findmode", "tags":{"scope":"private"}, "for" : {"editable":true}}, 
            "fontType" : {"type":"font","stringformat":true}, 
            "foreground" : "color", 
            "horizontalAlignment" : {"type" :"int", "tags": { "scope" :"design" }, "values" :[{"LEFT":2}, {"CENTER":0},{"RIGHT":4}],"default" : -1}, 
            "location" : {"type" :"point", "pushToServer": "deep"}, 
            "margin" : {"type" :"insets", "tags": { "scope" :"design" }}, 
            "placeholderText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
            "readOnly" : { "type" : "readOnly", "oppositeOf" : "editable"}, 
            "scrollbars" : {"type" :"scrollbars", "tags": { "scope" :"design" }}, 
            "size" : {"type" :"dimension",  "default" : {"width":370, "height":250}, "pushToServer": "deep"}, 
            "styleClass" : { "type" :"styleclass", "tags": { "scope" :"design" }, "values" :[]}, 
            "tabSeq" : {"type" :"tabseq", "tags": { "scope" :"design" }}, 
            "text" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
            "toolTipText" : { "type" : "tagstring", "displayTagsPropertyName" : "displaysTags" }, 
            "transparent" : "boolean", 
            "visible" : "visible" 
    },
    "handlers":
    {
            "onActionMethodID" : {
                
                "parameters":[
                                {
                                  "name":"event",
                                  "type":"JSEvent"
                                } 
                             ]
            }, 
            "onDataChangeMethodID" : {
              "returns": "boolean", 
                
                "parameters":[
                                {
                                  "name":"oldValue",
                                  "type":"${dataproviderType}"
                                }, 
                                {
                                  "name":"newValue",
                                  "type":"${dataproviderType}"
                                }, 
                                {
                                  "name":"event",
                                  "type":"JSEvent"
                                } 
                             ]
            }, 
            "onFocusGainedMethodID" : {
                
                "parameters":[
                                {
                                  "name":"event",
                                  "type":"JSEvent"
                                } 
                             ]
            }, 
            "onFocusLostMethodID" : {
                
                "parameters":[
                                {
                                  "name":"event",
                                  "type":"JSEvent"
                                } 
                             ]
            }, 
            "onRightClickMethodID" : {
                
                "parameters":[
                                {
                                  "name":"event",
                                  "type":"JSEvent"
                                } 
                             ]
            } 
    },
    "api":
    {
            "getAsPlainText": {
                "returns": "string"
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
            "getScrollX": {
                "returns": "int"
            },
            "getScrollY": {
                "returns": "int"
            },
            "getSelectedText": {
                "returns": "string"
            },
            "getWidth": {
                "returns": "int"
            },
            "replaceSelectedText": {
                "parameters":[
                                {                                                                 
                                "name":"s",
                                "type":"string"
                                }             
                             ]
    
            },
            "requestFocus": {
                "parameters":[
                                {                                                                 
                                "name":"mustExecuteOnFocusGainedMethod",
                                "type":"boolean",
                                "optional":true
                                }             
                             ],
                "delayUntilFormLoads": true,
            "discardPreviouslyQueuedSimilarCalls": true

            },
            "selectAll": {
                "delayUntilFormLoads": true,
            "discardPreviouslyQueuedSimilarCalls": true

            },
            "setScroll": {
                "parameters":[
                                {                                                                 
                                "name":"x",
                                "type":"int"
                                },
                                {                                                                 
                                "name":"y",
                                "type":"int"
                                }             
                             ]
    
            }
    }
     
}