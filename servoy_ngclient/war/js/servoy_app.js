var controllerProvider;
angular.module('servoyApp', ['servoy','webStorageModule','ngGrid','servoy-components', 'webSocketModule']).config(function($controllerProvider) {
	controllerProvider = $controllerProvider;
}).factory('$servoyInternal', function ($rootScope,$swingModifiers,webStorage,$anchorConstants, $q,$solutionSettings, $window, $webSocket) {
	   // formName:[beanname:{property1:1,property2:"test"}] needs to be synced to and from server
	   // this holds the form model with all the data, per form is this the "synced" view of the the IFormUI on the server 
	   // (3 way binding)
	   var formStates = {}; 
	   
	   var deferredProperties = {};
	   var deferredformStates = {};
	   var applyBeanData = function(beanModel, beanLayout, beanData, containerSize) {
            var anchorInfoChanged = false;
          	for(var key in beanData) {
          		switch(key)
          		{
          		case 'location':
          			beanLayout.left = beanData[key].x+'px';
          			beanLayout.top = beanData[key].y+'px';
                    anchorInfoChanged = true;
          			break;
          			
          		case 'size': 
          			beanLayout.width = beanData[key].width+'px';
          			beanLayout.height = beanData[key].height+'px';
          			anchorInfoChanged = true;
          			break;
          			
          		case 'anchors':
          			anchorInfoChanged = true;
          			break;
          			
          		case 'visible':
          			if (beanData[key] == false)
          			{
          				beanLayout.display = 'none';
          			}
          			else
          			{
          				delete beanLayout.display;
          			}
          			break;	
          		}
          		
          		// also make location and size available in model
          		beanModel[key] = beanData[key];
          	}
                
            if((beanModel.anchors !== undefined) && anchorInfoChanged && containerSize) {
                var anchoredTop = (beanModel.anchors & $anchorConstants.NORTH) != 0; // north
                var anchoredRight = (beanModel.anchors & $anchorConstants.EAST) != 0; // east
                var anchoredBottom = (beanModel.anchors & $anchorConstants.SOUTH) != 0; // south
                var anchoredLeft = (beanModel.anchors & $anchorConstants.WEST) != 0; //west
                
                if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
                if (!anchoredTop && !anchoredBottom) anchoredTop = true;
                
                if (anchoredTop) beanLayout.top = beanModel.location.y + 'px';
                else delete beanLayout.top;
                
                if (anchoredBottom) beanLayout.bottom = (containerSize.height - beanModel.location.y - beanModel.size.height) + "px";
                
                if (!anchoredTop || !anchoredBottom) beanLayout.height = beanModel.size.height + 'px';
                else delete beanLayout.height;
                
                if (anchoredLeft) beanLayout.left =  beanModel.location.x + 'px';
                else delete beanLayout.left;
                
                if (anchoredRight) beanLayout.right = (containerSize.width - beanModel.location.x - beanModel.size.width) + "px";
                
                if (!anchoredLeft || !anchoredRight) beanLayout.width = beanModel.size.width + 'px';
                else delete beanLayout.width;
            }
	   }
		   
	   // maybe do this with defer ($q)
	   var ignoreChanges = false;
	   $solutionSettings.solutionName  = /.*\/(\w+)\/.*/.exec(window.location.pathname)[1];
	   var wsSession = $webSocket.connect('client', webStorage.session.get("svyuuid"), $solutionSettings.solutionName)
       wsSession.onMessageObject = function (msg) {
		   try {
	        // data got back from the server
	        if (msg.forms) {
		        ignoreChanges = true;
	        	$rootScope.$apply(function() {
		          for(var formname in msg.forms) {
		        	// current model
		            var formState = formStates[formname];
		            // if the formState is on the server but not here anymore, skip it. 
		            // this can happen with a refresh on the browser.
		            if (!formState) continue;
		            var formModel = formState.model;
		            var layout = formState.layout;
		            var newFormData = msg.forms[formname];
		            var newFormProperties = newFormData['']; // get form properties

		            if(newFormProperties) {
		            	if (!formModel['']) formModel[''] = {};
		            	for(var p in newFormProperties) {
		            		formModel[''][p] = newFormProperties[p]; 
		    			} 
		            }

		            for(var beanname in newFormData) {
		            	// copy over the changes, skip for form properties (beanname empty)
		            	if(beanname != ''){
		            		applyBeanData(formModel[beanname], layout[beanname], newFormData[beanname], (newFormProperties && newFormProperties.size) ? newFormProperties.size : formState.properties.size);
		            		for (var defProperty in deferredProperties) {
		            			for(var key in newFormData[beanname]) {
		            				if (defProperty == (formname + "_" + beanname + "_" + key)) {
		            					deferredProperties[defProperty].resolve(newFormData[beanname][key]);
			            				delete deferredProperties[defProperty];
		            				}
		            			}
		            		} 
		            	}
		            }
		            if(deferredformStates[formname]){
			          deferredformStates[formname].resolve(formStates[formname])
			          delete deferredformStates[formname]
		            }
		          }
	        	});
		        ignoreChanges = false;
	        }
	        if (msg.call) {
	        	// {"call":{"form":"product","element":"datatextfield1","api":"requestFocus","args":[arg1, arg2]}, // optionally "viewIndex":1 
	        	// "{ conversions: {product: {datatextfield1: {0: "Date"}}} }
	        	var call = msg.call;
	        	var formState = formStates[call.form];
	        	if (call.viewIndex != undefined) {
	        		var funcThis = formState.api[call.bean][call.viewIndex]; 
	        		var func = funcThis[call.api];
	        	}
	        	else {
	        		var funcThis = formState.api[call.bean];
	        		var func = funcThis[call.api];
	        	}
	        	if (!func) {
	        		// if setFindMode not present, set editable/readonly state
	        		if (call.api != "setFindMode") 
	        		{
	        			console.warn("bean " + call.bean + " did not provide the api: " + call.api)
	        		}
	        		else
	        		{
	        			if (call.args[0])
	        			{
	        				formState.model[call.bean].readOnlyBeforeFindMode = formState.model[call.bean].readOnly;
	        				formState.model[call.bean].readOnly = true;
	        			}
	        			else
	        			{
	        				formState.model[call.bean].readOnly = formState.model[call.bean].readOnlyBeforeFindMode;
	        			}
	        		}
	        		return;
	        	}

	        	return $rootScope.$apply(function() {
	        		return func.apply(funcThis, call.args)
	        	})
	        }
	        if (msg.srvuuid) {
	        	webStorage.session.add("svyuuid",msg.srvuuid);
	        }
	        if (msg.styleSheetPath) {
	        	$solutionSettings.styleSheetPath = msg.styleSheetPath;
	        }
	        if (msg.windowName) {
	        	$solutionSettings.windowName = msg.windowName;
	        }
		   } finally {
			   ignoreChanges = false;
		   }
	    };
	    var getCombinedPropertyNames = function(now,prev) {
	       var fulllist = {}
    	   if (prev) {
	    	   var prevNames = Object.getOwnPropertyNames(prev);
	    	   for(var i=0;i<prevNames.length;i++) {
	    		   fulllist[prevNames[i]] = true;
	    	   }
    	   }
    	   if (now) {
	    	   var nowNames = Object.getOwnPropertyNames(now);
	    	   for(var i=0;i<nowNames.length;i++) {
	    		   fulllist[nowNames[i]] = true;
	    	   }
    	   }
    	   return fulllist;
	    }
	    
	   var isChanged = function(now, prev) {
		   if (now && prev) {
			   if (now instanceof Array) {
				   if (now.length != prev.length) return true;
			   }
			   if (now instanceof Date) {
				   if (prev instanceof Date) {
					   return now.getTime() != prev.getTime();
				   }
				   return true;
			   }
			   // first build up a list of all the properties both have.
	    	   var fulllist = getCombinedPropertyNames(now,prev);
	    	    for (var prop in fulllist) {
                    if(prop == "$$hashKey") continue; // ng repeat creates a child scope for each element in the array any scope has a $$hashKey property which must be ignored since it is not part of the model
	    	    	if (prev[prop] !== now[prop]) {
	    	    		if (typeof now[prop] == "object") {
	    	    			if (isChanged(now[prop],prev[prop])) {
	    	    				return true;
	    	    			}
	    	    		} else {
	    	               return true;
	    	    		}
	    	        }
	    	    }
	    	    return false;
		   }
		   return true;
	   }
	   
	   return {
		   getFormState: function(name){ 
			   var defered = null
			   if(!deferredformStates[name]){
				   var defered = $q.defer()
				   deferredformStates[name] = defered;
			   }else {
				   defered = deferredformStates[name]
			   }
			   
			   if(formStates[name]){
				   defered.resolve(formStates[name]); // then handlers are called even if they are applied after it is resolved
			   }			   
			   return defered.promise;
		   },
		   
		   clearformState: function(formName) {
			   delete formStates[formName];
		   },
	       initFormState: function(formName, beanDatas, formProperties) {
	        var state = formStates[formName];
	        // if the form is already initialized or if the beanDatas are not given, return that 
	        if (state != null || !beanDatas) return state; 
	        
	        // init all the objects for the beans.
	        var model = {};
	        var api = {};
	        var layout = {};

	        for(var beanname in beanDatas) {
            	// initialize with design nara
                    model[beanname] = {};
                    api[beanname] = {};
                    layout[beanname] = { position: 'absolute' }
                    applyBeanData(model[beanname], layout[beanname], beanDatas[beanname], formProperties.size)
                }

	        state = formStates[formName] = { model: model, api: api, layout: layout,
                            style: {
                            position: "absolute",
                            left: "0px",
                            top: "0px",
                            minWidth : formProperties.size.width + "px",
                            minHeight : formProperties.size.height + "px",
                            right: "0px",
                            bottom: "0px",
                            border: formProperties.border},
                            properties: formProperties};
	        
	        // send the special request initial data for this form 
	        // this can also make the form (IFormUI instance) on the server if that is not already done
	        wsSession.sendMessageObject({cmd:'requestdata',formname:formName});
	        return state;
	       },
	       getExecutor: function(formName) {
	          return {
	              on: function(beanname,eventName,property,args,svy_pk) {
	                  // this is onaction, onfocuslost which is really configured in the html so it really 
	                  // is something that goes to the server
	            	  var newargs = []
	            	  for (var i in args) {
	            		  var arg = args[i]
						  if (arg && arg.originalEvent) arg = arg.originalEvent;
 	                      if(arg  instanceof MouseEvent ||arg  instanceof KeyboardEvent){
	                    	var $event = arg;
	                    	var eventObj = {}
	                        var modifiers = 0;
	                        if($event.shiftKey) modifiers = modifiers||$swingModifiers.SHIFT_DOWN_MASK;
	                        if($event.metaKey) modifiers = modifiers||$swingModifiers.META_DOWN_MASK;
	                        if($event.altKey) modifiers = modifiers|| $swingModifiers.ALT_DOWN_MASK;
	                        if($event.ctrlKey) modifiers = modifiers || $swingModifiers.CTRL_DOWN_MASK;
	                          
	                        eventObj.type = 'event'; 
	                        eventObj.eventName = eventName; 
	                        eventObj.modifiers = modifiers;
	                        eventObj.timestamp = $event.timeStamp;
	                        eventObj.x= $event.pageX;
	                        eventObj.y= $event.pageY;
	                        arg = eventObj
	                      }
 	                      else if (arg instanceof Event || arg instanceof $.Event) {
							var eventObj = {}
	                        eventObj.type = 'event'; 
	                        eventObj.eventName = eventName; 
							eventObj.timestamp = arg.timeStamp;
							arg = eventObj
 	                      }
	                      newargs.push(arg)
	            	  }
	            	  var data = {}
	            	  if (property) {
	            		  data[property] = formStates[formName].model[beanname][property];
	            	  }
	            	  var cmd = {cmd:'event',formname:formName,beanname:beanname,event:eventName,args:newargs,changes:data}
	            	  if (svy_pk) cmd.svy_pk = svy_pk
	            	  return wsSession.sendDeferredMessage(cmd)
	              },
	          }
	       },
	       
	       sendRequest: function(objToStringify) {
	    	   wsSession.sendMessageObject(objToStringify);
	       },

	       sendChanges: function(now,prev,formname,beanname) {
	    	   if (ignoreChanges) return false;
	    	   // first build up a list of all the properties both have.
	    	   var fulllist = getCombinedPropertyNames(now,prev);
	    	    var changes = {}, prop;
	    	    for (prop in fulllist) {
	    	    	if (now[prop] == undefined) {
	    	    		changes[prop] = 'svy_undefined'; // special previous had a value that is removed from the current.
	    	    	}
	    	    	else if (!prev) {
	    	    		changes[prop] = $webSocket.convertClientObject(now[prop])
	    	    	}
	    	    	else if (prev[prop] !== now[prop]) {
	    	    		if (typeof now[prop] == "object") {
	    	    			if (isChanged(now[prop],prev[prop])) {
	    	    				changes[prop] = $webSocket.convertClientObject(now[prop]);
	    	    			}
	    	    		} else {
	    	               changes[prop] = $webSocket.convertClientObject(now[prop]);
	    	    		}
	    	        }
	    	    }
	    	    if (changes.location || changes.size || changes.visible || changes.anchors) {
	    	    	var beanLayout = formStates[formname].layout[beanname];
	    	    	if(beanLayout) {
	    	    		applyBeanData(formStates[formname].model[beanname], beanLayout, changes, formStates[formname].properties.size)	
	    	    	}
	    	    }

	    	    for (prop in changes) {
	    	    	wsSession.sendMessageObject({cmd:'datapush',formname:formname,beanname:beanname,changes:changes})
	    	    	return;
	    	    }
	    	},
	    	push: function(formname,beanname,property,beanModel) {	
	    		var changes = {}
	    		var clientObject;
	    		
	    		if(beanModel === undefined) {
	    			clientObject = formStates[formname].model[beanname][property];
	    		} else {
	    			clientObject = beanModel[property];
	    			if (beanModel.svy_pk){
	    				changes.svy_pk = beanModel.svy_pk;
	    			}
	    		}
	    		changes[property] = $webSocket.convertClientObject(clientObject);
	    	
	    		wsSession.sendMessageObject({cmd:'svypush',formname:formname,beanname:beanname,property:property,changes:changes})
	    	},
	    	filterList: function(formname,beanname,property,filter)  {
	    		var deferred = $q.defer();
	    		deferredProperties[formname + "_" + beanname + "_" + property] = deferred;
	    		wsSession.sendMessageObject({cmd:'valuelistfilter',formname:formname,beanname:beanname,property:property,filter:filter})
	    		return deferred.promise;
	    	},
	    	setFormVisibility: function(form,visible,relation, parentForm, bean,formIndex) {
	    		return wsSession.sendDeferredMessage({cmd:'formvisibility',form:form,visible:visible,parentForm:parentForm,bean:bean,relation:relation,formIndex:formIndex})
	    	},
	    	setFormEnabled: function(form,enabled) {
	    		wsSession.sendMessageObject({cmd:'formenabled',form:form,enabled:enabled})
	    	},
	    	setFormReadOnly: function(form,readOnly) {
	    		wsSession.sendMessageObject({cmd:'formreadOnly',form:form,readOnly:readOnly})
		    },
	    	callService: function(serviceName, methodName, argsObject) {
	    		return wsSession.callService(serviceName, methodName, argsObject)
	    	}
	   }
}).directive('svyLayoutUpdate', function($servoyInternal,$window,$timeout) {
    return {
      restrict: 'A', // only activate on element attribute
      link: function($scope, element, attrs) {
    	  var compModel;
    	  if(attrs['svyLayoutUpdate'].length == 0) {
    		  compModel = $scope.formProperties;
    	  } else {
    		  compModel = $scope.model[attrs['svyLayoutUpdate']];
    	  }

    	  if((attrs['svyLayoutUpdate'].length == 0) || (compModel.anchors !== undefined)) {
        	  var resizeTimeoutID = null;
        	  $window.addEventListener('resize',function() { 
        		  if(resizeTimeoutID) $timeout.cancel(resizeTimeoutID);
        		  resizeTimeoutID = $timeout( function() {
        			  if(compModel.location) {
        				  compModel.location.x = element.prop('offsetLeft');
        				  compModel.location.y = element.prop('offsetTop');
        			  }
        			  if(compModel.size) {
            			  compModel.size.width = element.prop('offsetWidth');
            			  compModel.size.height = element.prop('offsetHeight');  
        			  }
        		  }, 1000);
        	  });
    	  }
      }
    };   
}).value("$solutionSettings",  {
	mainForm: {},
	navigatorForm: {width:0},
	solutionTitle: "",
	defaultNavigatorState: {max:0,currentIdx:0,form:'<none>'},
	styleSheetPath: undefined
}).controller("MainController", function($scope, $solutionSettings, $servoyInternal, $windowService) {
	$scope.solutionSettings = $solutionSettings;
	$scope.getMainFormUrl = function() {
		return $solutionSettings.mainForm.templateURL?$windowService.getFormUrl($solutionSettings.mainForm.templateURL):"";
	}
	$scope.getNavigatorFormUrl = function() {
		if ( $solutionSettings.navigatorForm.templateURL && $solutionSettings.navigatorForm.templateURL.lastIndexOf("default_navigator_container.html") == -1) {
			return $windowService.getFormUrl($solutionSettings.navigatorForm.templateURL);
		}
		return $solutionSettings.navigatorForm.templateURL;
	}
	
}).factory("$applicationService", function(webStorage) {
	
	return {
		getUserProperty: function(key) {
			var json = webStorage.local.get("userProperties");
			if (json) {
				return JSON.parse(json)[key];
			}
			return null;
		},
		setUserProperty: function(key,value) {
			var obj = {}
			var json = webStorage.local.get("userProperties");
			if (json) {
				obj = JSON.parse(json);
			}
			if (value == null) delete obj[key]
			else obj[key] = value;
			webStorage.local.add("userProperties", JSON.stringify(obj))
		},
		getUserPropertyNames: function() {
			var json = webStorage.local.get("userProperties");
			if (json) {
				return Object.getOwnPropertyNames(JSON.parse(json));
			}
			return [];
		}
	}
	
}).factory("$windowService", function($modal, $log, $templateCache, $rootScope, $solutionSettings, $window, $servoyInternal) {
	var instances = {};
	
	var formTemplateUrls = {};
	
	 $templateCache.put("template/modal/window.html",
			    "<div tabindex=\"-1\" class=\"modal fade {{ windowClass }}\" ng-class=\"{in: animate}\" ng-style=\"{'z-index': 1050 + index*10, display: 'block'}\">\n" +
			    "    <div class=\"modal-dialog\" ng-style=\"formSize\"><div class=\"modal-content\" ng-style=\"formSize\" ng-transclude></div></div>\n" +
			    "</div>");
	return {
		show: function(name,dialogDescription) {
			if (!instances[name]) {
				var modalInstance = $modal.open({
					templateUrl: "templates/modaldialog.html",
					controller: "DialogInstanceCtrl",
					windowClass: "tester",
					resolve: {
						title: function () {
							return dialogDescription.title;
						},
						form: function () {
							return dialogDescription.form;
						},
						windowName: function() {
							return name;
						},
						formSize: function() {
							return dialogDescription.size;
						}
					}
				});
				modalInstance.form = dialogDescription.form;
				modalInstance.size = dialogDescription.size;
				instances[name] = modalInstance;
			}
			else {
				$log.error("modal dialog with name: " + name + " already showing");
			}
		},
		dismiss: function(name) {
			var instance = instances[name];
			if (instance) {
				instance.dismiss();
				delete instances[name];
			}
		},
		switchForm: function(name,mainForm,navigatorForm) {		
        	$rootScope.$apply(function() { // TODO treat multiple windows case
        		if($solutionSettings.windowName == name) { // main window form switch
        			$solutionSettings.mainForm = mainForm;
        			$solutionSettings.navigatorForm = navigatorForm;
        		}
    		})
		},
		setTitle: function(title) {		
        	$rootScope.$apply(function() { // TODO treat multiple windows case
        		$solutionSettings.solutionTitle = title;
    		})
		},
		reload: function() {
			$rootScope.$apply(function() {
        		$window.location.reload(true);
    		})
		},
		updateController: function(formName,controllerCode, realFormUrl) {
			$rootScope.$apply(function() {
				$servoyInternal.clearformState(formName)
				eval(controllerCode);
				formTemplateUrls[formName] = realFormUrl;
			});
		},
 		getFormUrl: function(formName) {
			var realFormUrl = formTemplateUrls[formName];
			if (realFormUrl == null) {
					$servoyInternal.callService("$windowService", "touchForm", {name:formName});
			}
			return realFormUrl;
		},
	}
	
}).directive('modalWindow', ['$modalStack', '$timeout', function ($modalStack, $timeout) {
    return {
        restrict: 'EA',
        link: function (scope, element, attrs) {
          scope.$$childHead.formSize = scope.formSize; 
        }
      };
}]).controller("DialogInstanceCtrl", function ($scope, $modalInstance,$windowService, $servoyInternal,windowName,title,form,formSize) {
	$scope.title = title;
	$scope.windowName = windowName;
	$scope.formSize =formSize;
	$scope.getFormUrl = function() {
		return $windowService.getFormUrl(form)
	}
	
	$servoyInternal.setFormVisibility(form,true);
	
	$scope.cancel = function () {
		var promise = $servoyInternal.callService("$windowService", "windowClosing", {window:windowName});
		promise.then(function(ok) {
    		if (ok) {
    			$windowService.dismiss(windowName);
    		}
    	})
	};
}).run(function($window, $servoyInternal) {
	$window.executeInlineScript = function(formname, script, params) {
		$servoyInternal.callService("formService", "executeInlineScript", {'formname' : formname, 'script' : script, 'params' : params})
	}
})