{
    "name": "window",
    "displayName": "Servoy Window plugin",
    "version": 1,
    "definition": "servoydefaultservices/window/window.js",
    "serverscript": "servoydefaultservices/window/window_server.js",
    "doc": "servoydefaultservices/window/window_doc.js",
    "ng2Config": {
       "packageName": "@servoy/window",
       "moduleName": "WindowServiceModule",
       "serviceName": "WindowPluginService",
       "entryPoint": "projects/window",
       "dependencies": {
            "csslibrary" : ["~@servoy/window/servoy-menu.css"]
        }
    },
    "libraries": [{"name":"window/shortcut.js", "version":"1", "url":"servoydefaultservices/window/shortcut.js", "mimetype":"text/javascript"},{"name":"yahoo-dom-event.js", "version":"2.9.0", "url":"servoydefaultservices/window/yahoo-dom-event.js", "mimetype":"text/javascript"},{"name":"window/container_core.js", "version":"2.9.0", "url":"servoydefaultservices/window/container_core-min.js", "mimetype":"text/javascript"},{"name":"menu.js", "version":"2.9.0", "url":"servoydefaultservices/window/menu-min.js", "mimetype":"text/javascript"},{"name":"menu.css", "version":"2.9.0", "url":"servoydefaultservices/window/menu.css", "mimetype":"text/css"},{"name":"servoy-menu.css", "version":"1", "url":"servoydefaultservices/window/servoy-menu.css", "mimetype":"text/css"}],
    "model":
    {
        "shortcuts" : { "type": "shortcut[]", "tags": { "scope" :"private" }},
        "popupform": {"type": "popupform", "tags": { "scope" :"private" }},
        "popupMenus" : {"type": "Popup[]", "tags": { "scope" :"private" }},
        "popupMenuShowCommand" : {"type": "popupMenuShowCommand", "pushToServer": "shallow", "tags": { "scope" :"private" }} 
    },
    "api":
    {
         "createShortcut": {
                "returns": "boolean",
                "parameters":[
                                {
                                    "name":"shortcut",
                                    "type":"string"
                                },
                                {
                                    "name":"callback",
                                    "type":"function"
                                },
                                {
                                    "name":"contextFilter",
                                    "type":"string",
                                    "optional":true
                                },
                                {
                                    "name":"arguments",
                                    "type":"object []",
                                    "optional":true
                                },
                                {
                                    "name":"consumeEvent",
                                    "type":"boolean",
                                    "optional":true
                                }
                             ]
            },
         "removeShortcut": {
                "returns": "boolean",
                "parameters":[
                                {
                                    "name":"shortcut",
                                    "type":"string"
                                },
                                {
                                    "name":"contextFilter",
                                    "type":"string",
                                    "optional":true
                                }
                             ]
            },
         "showFormPopup": {
                "parameters":[
                                {
                                    "name":"component",
                                    "type":"runtimecomponent"
                                },
                                {
                                    "name":"form",
                                    "type":"form"
                                },
                                {
                                    "name":"scope",
                                    "type":"object"
                                },
                                {
                                    "name":"dataProviderID",
                                    "type":"string"
                                },
                                {
                                    "name":"width",
                                    "type":"int",
                                    "optional":true
                                },
                                {
                                    "name":"height",
                                    "type":"int",
                                    "optional":true
                                    
                                },
                                {
                                    "name":"x",
                                    "type":"int",
                                    "optional":true
                                },
                                {
                                    "name":"y",
                                    "type":"int",
                                    "optional":true
                                    
                                },
                                {
                                    "name":"showBackdrop",
                                    "type":"boolean",
                                    "optional":true
                                    
                                },
                                {
                                    "name":"doNotCloseOnClickOutside",
                                    "type":"boolean",
                                    "optional":true
                                    
                                },
                                {
                                    "name":"onClose",
                                    "type":"function",
                                    "optional":true
                                }
                             ]
            },
         "closeFormPopup": {
                "parameters":[
                                {
                                "name":"retval",
                                "type":"object"
                                }
                             ]
            },
         "cancelFormPopup": {
            },
         "createFormPopup": {
             "parameters":[
                {
                    "name":"component",
                    "type":"runtimecomponent"
                },
                {
                    "name":"form",
                    "type":"form"
                }
               ],
              "returns": "FormPopup"
          },
         "createPopupMenu": {
               "parameters":[
                {
                    "name":"menu",
                    "type":"JSMenu",
                    "optional":true
                },
                {
                     "name":"callback",
                     "type":"function",
                     "optional":true
                }
               ],
               "returns": "Popup"
            },   
         "cleanup": {
            }
    },
    "internalApi":
    {
         "formPopupClosed": {
            "parameters":[
                    {
                        "name":"event",
                        "type":"JSEvent"
                    }
                ]
             }, 
        "clearPopupForm" :{
        },
        "cancelFormPopupInternal": {
            "parameters":[
                {
                    "name":"disableClearPopupFormCallToServer",
                    "type":"boolean"
                }
            ]
        },
         "executeMenuItem" :{
               "parameters":[
                 {
                    "name":"menuItemId",
                    "type":"string"
                 },
                 {
                    "name":"itemIndex",
                    "type":"int"
                 },
                 {
                    "name":"parentItemIndex",
                    "type":"int"
                 },
                 {
                    "name":"isSelected",
                    "type":"boolean"
                 },
                 {
                    "name":"parentMenuText",
                    "type":"string"
                 },
                 {
                    "name":"menuText",
                    "type":"string"
                 }
            ]
        }
    },
    "types": {
      "shortcut": {
        "model": {
            "shortcut": "string",
            "callback": "function",
            "contextFilter": "string",
            "arguments": "object[]"
        }
      },
      "popupform": {
        "model": {
            "component": "runtimecomponent",
            "form": "form",
            "width": "int",
            "height": "int",
            "x": "int",
            "y": "int",
            "showBackdrop": "boolean",
            "doNotCloseOnClickOutside": "boolean",
            "onClose": "function",
            "parent": "popupform"
        }
      },
      "popupMenuShowCommand":{
          "model": {
            "popupName": "string",
            "elementId": "string",
            "x": "int",
            "y": "int",
            "height": "int",
            "positionTop": "boolean"
        }
      },
      "FormPopup": {
        "model": {
            "component": {"type" : "runtimecomponent", "tags": { "scope" :"private" }},
            "dataprovider" : {"type" : "string", "tags": { "scope" :"private" }},
            "scope": {"type" : "object", "tags": { "scope" :"private" }},
            "width": {"type" : "int", "tags": { "scope" :"private" }},
            "height": {"type" : "int", "tags": { "scope" :"private" }},
            "x": {"type" : "int", "tags": { "scope" :"private" }},
            "y": {"type" : "int", "tags": { "scope" :"private" }},
            "showBackdrop": {"type" : "boolean", "tags": { "scope" :"private" }},
            "doNotCloseOnClickOutside": {"type" : "boolean", "tags": { "scope" :"private" }},
            "onClose": {"type" : "function", "tags": { "scope" :"private" }}
        },
        "serversideapi": {
            "component": {
                "parameters":[],
                "returns": "runtimecomponent",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "component",
                            "type": "runtimecomponent"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "width": {
                "parameters":[],
                "returns": "int",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "width",
                            "type": "int"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "height": {
                "parameters":[],
                "returns": "int",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "height",
                            "type": "int"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "x": {
                "parameters":[],
                "returns": "int",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "x",
                            "type": "int"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "y": {
                "parameters":[],
                "returns": "int",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "y",
                            "type": "int"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "showBackdrop": {
                "parameters":[],
                "returns": "boolean",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "showBackdrop",
                            "type": "boolean"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "dataprovider": {
                "parameters":[],
                "returns": "string",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "dataprovider",
                            "type": "string"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "onClose": {
                "parameters":[],
                "returns": "function",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "onClose",
                            "type": "function"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "scope": {
                "parameters":[],
                "returns": "object",
                "overloads": [
                    {
                        "parameters": [
                        {
                            "name": "scope",
                            "type": "object"
                        }],
                        "returns": "FormPopup"
                    }
                ]
            },
            "show": {
                "parameters":[],
            }
        }
      },
      "MenuItem": {
        "model":{
            "name": "string",
            "methodArguments": "object[]",
            "text": "string",
            "selected": "boolean",
            "enabled": { "type": "protected", "blockingOn": false, "default": true },
            "id":{"type" : "string", "tags": { "scope" :"private" }},
            "callback": {"type" : "function", "tags": { "scope" :"private" }},
            "align": {"type" : "int", "tags": { "scope" :"private" }},
            "visible": {"type" : "visible", "tags": { "scope" :"private" }},
            "icon": {"type" : "media", "tags": { "scope" :"private" }},
            "fa_icon": {"type" : "string", "tags": { "scope" :"private" }},
            "mnemonic": {"type" : "string", "tags": { "scope" :"private" }},
            "backgroundColor": {"type" : "string", "tags": { "scope" :"private" }},
            "foregroundColor": {"type" : "string", "tags": { "scope" :"private" }},
            "accelarator": {"type" : "string", "tags": { "scope" :"private" }},
            "cssClass": {"type" : "string", "tags": { "scope" :"private" }},
            "items":  {"type" : "MenuItem[]", "tags": { "scope" :"private" }}
        },
        "serversideapi": {
           "doClick": {
             "parameters":[]
            },
            "setMethod": {
             "parameters":[
                {
                    "name":"method",
                    "type":"function"
                },
                {
                    "name":"arguments",
                    "type":"object[]",
                    "optional":true
                }
             ]
            },
            "setAccelerator": {
             "parameters":[
                {
                    "name":"accelerator",
                    "type":"string"
                }
             ]
            },
            "setIcon": {
             "parameters":[
                {
                    "name":"icon",
                    "type":"object"
                }
             ]
            },
            "setMnemonic": {
             "parameters":[
                {
                    "name":"mnemonic",
                    "type":"string"
                }
             ]
            },
            "setBackgroundColor": {
             "parameters":[
                {
                    "name":"bgColor",
                    "type":"string"
                }
             ]
            },
            "setForegroundColor": {
             "parameters":[
                {
                    "name":"fgColor",
                    "type":"string"
                }
             ]
            },
            "putClientProperty": {
             "parameters":[
                {
                    "name":"key",
                    "type":"object"
                },
                {
                    "name":"value",
                    "type":"object"
                }
             ]
            },
            "getClientProperty": {
             "parameters":[
                {
                    "name":"key",
                    "type":"object"
                }
             ]
            }
        }
      },
      "CheckBox" : {
        "extends": "MenuItem",
        "model":{
        }
      },
      "RadioButton" : {
        "extends": "MenuItem",
        "model":{
        }
      },
      "BaseMenu": {
        "serversideapi":
        {
         "addMenu": {
             "parameters":[
                {
                    "name":"name",
                    "type":"string",
                    "optional":true
                }
            ],
            "returns": "Menu"
         },        
         "addMenuItem": {
             "parameters":[
                {
                    "name":"name",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"feedback_item",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"icon",
                    "type":"object",
                    "optional":true
                },
                {
                    "name":"mnemonic",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"enabled",
                    "type":"boolean",
                    "optional":true
                },
                {
                    "name":"align",
                    "type":"int",
                    "optional":true
                }
               ],
              "returns": "MenuItem"
            },
            "addCheckBox": {
             "parameters":[
                {
                    "name":"name",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"feedback_item",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"icon",
                    "type":"object",
                    "optional":true
                },
                {
                    "name":"mnemonic",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"enabled",
                    "type":"boolean",
                    "optional":true
                },
                {
                    "name":"align",
                    "type":"int",
                    "optional":true
                }
               ],
              "returns": "CheckBox"
            },
            "addRadioGroup": {
            },
            "addRadioButton": {
             "parameters":[
                {
                    "name":"name",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"feedback_item",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"icon",
                    "type":"object",
                    "optional":true
                },
                {
                    "name":"mnemonic",
                    "type":"string",
                    "optional":true
                },
                {
                    "name":"enabled",
                    "type":"boolean",
                    "optional":true
                },
                {
                    "name":"align",
                    "type":"int",
                    "optional":true
                }
               ],
              "returns": "RadioButton"
            },
            "addSeparator": {
                "parameters":[
                    {
                        "name":"index",
                        "type":"int",
                        "optional":true
                    }
                ]
            },
            "getCheckBox": {
               "parameters":[
                   {
                       "name":"index",
                       "type":"int"
                   }
               ],
               "returns": "CheckBox"
            },
            "getRadioButton": {
               "parameters":[
                   {
                       "name":"index",
                       "type":"int"
                   }
               ],
               "returns": "RadioButton"
            },
            "getItem": {
               "parameters":[
                   {
                       "name":"index",
                       "type":"int"
                   }
               ],
               "returns": "MenuItem"
            },
            "getItemCount": {
               "parameters":[],
               "returns": "int"
            },
            "getItemIndexByText": {
               "parameters":[
                   {
                       "name":"text",
                       "type":"string"
                   }
               ],
               "returns": "MenuItem"
            },
            "getMenu": {
               "parameters":[
                   {
                       "name":"index",
                       "type":"int"
                   }
               ],
               "returns": "Menu"
            },
            "removeAllItems": {
               "parameters":[]
            },
            "removeItem": {
               "parameters":[
                   {
                       "name":"indices",
                       "type":"object[]"
                   }
               ]
            },
            "putClientProperty": {
             "parameters":[
                {
                    "name":"key",
                    "type":"object"
                },
                {
                    "name":"value",
                    "type":"object"
                }
             ]
            },
            "getClientProperty": {
             "parameters":[
                {
                    "name":"key",
                    "type":"object"
                }
             ],
             "returns": "object"
            }
        }      
      },
      "Menu": {
          "extends": "BaseMenu",
          "model": {
            "text" : "string"
          },
          "serversideapi": {
            "doClick": {
             "parameters":[]
            },
            "setEnabled": {
                "parameters":[{
                    "name":"enabled",
                    "type":"boolean"
                    
                }]
            },
            "setIcon": {
                "parameters":[{
                    "name":"icon",
                    "type":"object"
                    
                }]
            },
            "setMnemonic": {
                "parameters":[{
                    "name":"mnemonic",
                    "type":"string"
                    
                }]
            }
       }
      },
      "Popup": {
        "extends": "BaseMenu",
        "model": {
            "cssClass" : "string",
            "name": {"type" : "string", "tags": { "scope" :"private" }},
            "items":  {"type" : "MenuItem[]", "tags": { "scope" :"private" }}
        },
        "serversideapi": {
            "show": {
             "parameters":[{
                "name": "component",
                "type": "runtimecomponent"
             },
              {
                "name": "x",
                "type": "int"
             },
             {
                 "name": "y",
                 "type": "int"
              },
              {
                 "name": "positionTop",
                 "type": "boolean",
                 "optional": true
              }],
              "overloads" : [
                {
                    "parameters": [{
                      "name": "component",
                      "type": "runtimecomponent"
                    },
                    {
                      "name": "positionTop",
                      "type": "boolean",
                      "optional": true
                    }]
                },
                {
                    "parameters": [{
                      "name": "x",
                      "type": "int"
                    },
                    {
                      "name": "y",
                      "type": "int"
                    }]
                },
                {
                    "parameters": [{
                      "name": "event",
                      "type": "JSEvent"
                    }]
                }
              ]
            }
        }
      }
    }
}
