var servoyModule = angular.module('servoy', ['webStorageModule','ui.bootstrap','ngGrid'])
.factory('$servoy', function ($rootScope,$swingModifiers,webStorage,$anchorConstants, $q,$solutionSettings, $window,$injector) {
	   // formName:[beanname:{property1:1,property2:"test"}] needs to be synced to and from server
	   // this holds the form model with all the data, per form is this the "synced" view of the the IFormUI on the server 
	   // (3 way binding)
	   var formStates = {}; 
	   
	   var convertServerObject = function(value, toType) {
		   if (toType == "Date") {
			   value = new Date(value);
		   }
		   return value;
	   }
	   
	   var convertClientObject = function(value) {
		   if (value instanceof Date) {
			   value = value.getTime();
		   }
		   return value;
	   } 
	   
	   var deferredProperties = {};
	   var deferredformStates = {};
	   var deferredEvents = {};
	   var nextMessageId = 1
	   var getNextMessageId = function() {
		   return nextMessageId++
	   }
	   
	   var applyBeanData = function(beanModel, beanLayout, beanData, containerSize) {
            var anchorInfoChanged = false;
          	for(var key in beanData) {
          		switch(key)
          		{
          		case 'location': 
          			beanLayout.left = beanData[key].x+'px';
          			beanLayout.top = beanData[key].y+'px';
                    anchorInfoChanged = anchorInfoChanged || (beanModel[key] == undefined) || (beanModel[key].x != beanData[key].x) || (beanModel[key].y != beanData[key].y);
          			break;
          			
          		case 'size': 
          			beanLayout.width = beanData[key].width+'px';
          			beanLayout.height = beanData[key].height+'px';
          			anchorInfoChanged = anchorInfoChanged || (beanModel[key] == undefined) || (beanModel[key].width != beanData[key].width) || (beanModel[key].height != beanData[key].height);
          			break;
          		case 'anchors':
          			anchorInfoChanged = anchorInfoChanged || (beanModel[key] == undefined) || (beanData[key] != beanModel[key]);
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
	   var loc = window.location, new_uri;
	   if (loc.protocol === "https:") {
	       new_uri = "wss:";
	   } else {
	       new_uri = "ws:";
	   }
	   new_uri += "//" + loc.host;
	   var lastIndex = loc.pathname.lastIndexOf("/");
	   $solutionSettings.solutionName  = /.*\/(\w+)\/.*/.exec(loc.pathname)[1];
	   if (lastIndex > 0) {
		   new_uri += loc.pathname.substring(0,lastIndex) + "/../../websocket";   // path of index.html is like so http://localhost:8080/webclient2/solutions/{solName}/index.html
	   }
	   else {
		   new_uri += loc.pathname + "/websocket";
	   }
	   
	   var websocket = new WebSocket(new_uri);
	   websocket.onopen = function () {
           console.log('Info: WebSocket connection opened.');
           var initcmd = {cmd:'init', solutionName: $solutionSettings.solutionName}
           var srvuuid = webStorage.session.get("svyuuid");
           if (srvuuid != null)
    	   {
        	   initcmd.srvuuid = srvuuid;
    	   }
           websocket.send(JSON.stringify(initcmd));
       };
       websocket.onerror = function(message) {
    	   console.error('Error: WebSocket on error: ' + message);
       }
       websocket.onclose = function(message) {
    	   console.log('Info: WebSocket on close, code: ' + message.code + ' , reason: ' + message.reason);
       }
       websocket.onmessage = function (message) {
		   try {
			ignoreChanges = true;
	        var obj = JSON.parse(message.data);
	        var conversions = {};
	        if (obj.conversions) conversions = obj.conversions; 
	    	var applyConversion = function(data, conversion) {
        		for(var conKey in conversion) {
        			if (conversion[conKey] == "Date") {
        				data[conKey] = convertServerObject(data[conKey], conversion[conKey]);
        			}
        			else {
        				applyConversion(data[conKey],conversion[conKey]);
        			}
        		}
        	} 
        	
        	applyConversion(obj, conversions)

	        // data got back from the server
	        if (obj.forms) {
	        	// TODO check is it better to call apply on the form scopes that are changed? (performance?)
	        	$rootScope.$apply(function() {
		          for(var formname in obj.forms) {
		        	// current model
		            var formState = formStates[formname];
		            // if the formState is on the server but not here anymore, skip it. 
		            // this can happen with a refresh on the browser.
		            if (!formState) continue;
		            var formModel = formState.model;
		            var layout = formState.layout;
		            var newFormData = obj.forms[formname];
		            var formConversion = conversions[formname];
		            var newFormProperties = newFormData['']; // get form properties
		            var rowConversions = formConversion ? formConversion[''] : undefined

		            if(newFormProperties) {
		            	if (!formModel['']) formModel[''] = {};
		            	for(var p in newFormProperties) {
		            		formModel[''][p] = newFormProperties[p]; 
		    			} 
		            }
	            	
		            for(var beanname in newFormData) {
		            	// copy over the changes, skip for form properties (beanname empty)
		            	if(beanname != ''){
		            		applyBeanData(formModel[beanname], layout[beanname], newFormData[beanname], newFormProperties ? newFormProperties.size : formState.properties.size);
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
	        }
	        if (obj.call) {
	        	// {"call":{"form":"product","element":"datatextfield1","api":"requestFocus","args":[arg1, arg2]}, // optionally "viewIndex":1 
	        	// "{ conversions: {product: {datatextfield1: {0: "Date"}}} }
	        	var call = obj.call;
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
	        		console.error("bean " + call.bean + " did not provide the api: " + call.api)
	        		return;
	        	}

	        	var args = call.args
	        	// conversion
	        	if (args && call.conversions && call.conversions.args) {
	        		  args = args.slice(0)
	        		  var argsConversions = call.conversions.args
	        		  for (var conv in argsConversions)
        			  {
	        			  var index = parseInt(conv)
	        			  args[index] = convertServerObject(args[index], argsConversions[conv])
        			  }
		          }
	        	
	        	$rootScope.$apply(function() {
	        		var ret = func.apply(funcThis, args)
		        	if (obj.smsgid) {
		        		// server wants a response
		        		var response = {cmd: 'response', smsgid: obj.smsgid}
		        		if (ret) response.ret = convertClientObject(ret);
		        		websocket.send(JSON.stringify(response));
		        	}
	        	})
	        }
	        if (obj.cmsgid) { // response to event
	        	if (obj.exception) {
	        		// something went wrong
	        		$rootScope.$apply(function() {
	        			deferredEvents[obj.cmsgid].reject(obj.exception);
	        		})
	        	}
	        	else {
	        		$rootScope.$apply(function() {
	        			deferredEvents[obj.cmsgid].resolve(obj.ret);
	        		})
	        	}
				delete deferredEvents[obj.cmsgid];
	        }
	        if (obj.srvuuid) {
	        	webStorage.session.add("svyuuid",obj.srvuuid);
	        }
	        if (obj.windowName) {
	        	$solutionSettings.windowName = obj.windowName;
	        }
	        if (obj.services) {
	        	for (var index in obj.services)
	        	{
	        		var service = obj.services[index];
	        		var serviceInstance = $injector.get(service.name);
	        		if (serviceInstance && serviceInstance[service.call]) {
	        			var ret = serviceInstance[service.call].apply(serviceInstance,service.args);
	        			
	        			if (service.smsgid) {
			        		// server wants a response
			        		var response = {cmd: 'response', smsgid: service.smsgid}
			        		if (ret) response.ret = convertClientObject(ret);
			        		websocket.send(JSON.stringify(response));
			        	}
	        		}
	        	}
	        	
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
	        websocket.send(JSON.stringify({cmd:'requestdata',formname:formName}));
	        return state;
	       },
	       getExecutor: function(formName) {
	          return {
	              on: function(beanname,eventName,property,args,svy_pk) {
	                  // this is onaction, onfocuslost which is really configured in the html so it really 
	                  // is something that goes to the server
	            	  var newargs = []
	            	  for (var i in args) {
	            		  var arg;
	                      if(args[i]  instanceof MouseEvent){
	                    	var $event = args[i]
	                    	var eventObj = {}
	                        var modifiers = 0;
	                        if($event.shiftKey) modifiers = modifiers||$swingModifiers.SHIFT_DOWN_MASK;
	                        if($event.metaKey) modifiers = modifiers||$swingModifiers.META_DOWN_MASK;
	                        if($event.altKey) modifiers = modifiers|| $swingModifiers.ALT_DOWN_MASK;
	                        if($event.ctrlKey) modifiers = modifiers || $swingModifiers.CTRL_DOWN_MASK;
	                        if($event.ctrlKey) modifiers = modifiers || $swingModifiers.CTRL_DOWN_MASK;
	                          
	                        eventObj.type = 'event'; 
	                        eventObj.eventName = eventName; 
	                        eventObj.modifiers = modifiers;
	                        eventObj.timestamp = $event.timeStamp;
	                        eventObj.x= $event.pageX;
	                        eventObj.y= $event.pageY;
	                        arg = eventObj
	                      }
	                      else {
	                    	arg = args[i]
	                      }
	                      newargs.push(arg)
	            	  }
	            	  var data = {}
	            	  if (property) {
	            		  data[property] = formStates[formName].model[beanname][property];
	            	  }
	            	  
	            	  var deferred = $q.defer();
	            	  var cmsgid = getNextMessageId()
	            	  deferredEvents[cmsgid] = deferred;
	            	  var cmd = {cmd:'event', cmsgid:cmsgid,formname:formName,beanname:beanname,event:eventName,args:newargs,changes:data}
	            	  if (svy_pk) cmd.svy_pk = svy_pk
	            	  websocket.send(JSON.stringify(cmd))
	            	  return deferred.promise;
	              },
	          }
	       },

	       sendRequest: function(stringifyjson) {
	    	   websocket.send(stringifyjson);
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
	    	    		changes[prop] = convertClientObject(now[prop])
	    	    	}
	    	    	else if (prev[prop] !== now[prop]) {
	    	    		if (typeof now[prop] == "object") {
	    	    			if (isChanged(now[prop],prev[prop])) {
	    	    				changes[prop] = convertClientObject(now[prop]);
	    	    			}
	    	    		} else {
	    	               changes[prop] = convertClientObject(now[prop]);
	    	    		}
	    	        }
	    	    }
	    	    for (prop in changes) {
	    	    	websocket.send(JSON.stringify({cmd:'datapush',formname:formname,beanname:beanname,changes:changes}))
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
	    		changes[property] = convertClientObject(clientObject);
	    	
	    		websocket.send(JSON.stringify({cmd:'svypush',formname:formname,beanname:beanname,property:property,changes:changes}))
	    	},
	    	filterList: function(formname,beanname,property,filter)  {
	    		var deferred = $q.defer();
	    		deferredProperties[formname + "_" + beanname + "_" + property] = deferred;
	    		websocket.send(JSON.stringify({cmd:'valuelistfilter',formname:formname,beanname:beanname,property:property,filter:filter}))
	    		return deferred.promise;
	    	},
	    	setFormVisibility: function(form,visible,relation, parentForm, bean) {
	    		  var deferred = $q.defer();
            	  var cmsgid = getNextMessageId()
            	  deferredEvents[cmsgid] = deferred;
            	  var cmd = {cmd:'formvisibility', cmsgid:cmsgid,form:form,visible:visible,parentForm:parentForm,bean:bean,relation:relation}
            	  websocket.send(JSON.stringify(cmd))
            	  return deferred.promise;
	    	},
	    	callService: function(serviceName, methodName, argsObject) {
	    		var deferred = $q.defer();
	    		var cmsgid = getNextMessageId()
	    		deferredEvents[cmsgid] = deferred;
	    		var cmd = {cmd:'service', cmsgid:cmsgid,servicename:serviceName,methodname:methodName,args:argsObject}
	    		websocket.send(JSON.stringify(cmd))
	    		return deferred.promise;
	    	}
	   }
}).directive('ngOnChange', function($parse){
    return function(scope, elm, attrs){       
        var onChangeFunction = $parse(attrs['ngOnChange']);
        elm.bind("change", function(event) {
            scope.$apply(function() {
            	onChangeFunction(scope, { $cmd: event });
            })});
    };
}).directive('svyAutoapply', function($servoy,$parse,$log) {
    return {
      restrict: 'A', // only activate on element attribute
      require: '?ngModel', // get a hold of NgModelController
      link: function(scope, element, attrs, ngModel) {
        if(!ngModel) return; // do nothing if no ng-model

        var dataproviderString = attrs.ngModel;
        var index = dataproviderString.indexOf('.');
        if (index > 0) {
	        var modelString = dataproviderString.substring(0,index);
	        var modelFunction = $parse(modelString);
	        var beanModel = modelFunction(scope);
	        var propertyname = dataproviderString.substring(index+1);
	        var beanname;
	        var parent = scope.$parent;
	        
	        if(beanModel.svy_cn === undefined) {
		        for(key in parent.model) {
		        	if (parent.model[key] === beanModel) {
		        		beanname = key;
		        		break;
		        	}
		        }	
	        } else {
	        	beanname = beanModel.svy_cn;
	        }
	        
	        if (!beanname) {
	        	$log.error("bean name not found for model string: " + dataproviderString);
	        	return;
	        }
	        var formname = parent.formname;
	        while (!formname) {
	        	if (parent) {
	        		parent = parent.$parent;
	        		formname = parent.formname;
	        	}
	        	else { 
	        		$log.error("no form found for " + bean + "." + propertyname);
	        		return;
	        	}
	        }
	        
	        // Listen for change events to enable binding
	        element.bind('change', function() {
	        	// model has not been updated yet
	        	setTimeout(function() { 
	        		$servoy.push(formname,beanname,propertyname,modelFunction(scope))}, 0);
	        });
	        // Listen for start edit
  	        element.bind('focus', function() {
  	        	setTimeout(function() { 
  	        		$servoy.callService("formService", "startEdit", {formname:formname,beanname:beanname,property:propertyname})}, 0);
  	        });
        }
        else {
        	$log.error("svyAutoapply attached to a element that doesn't have the right ngmodel (model.value): " + dataproviderString)
        }
      }
    };
}).directive('svyLayoutUpdate', function($servoy,$window,$timeout) {
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
}).directive('ngBlur', ['$parse', function($parse) {
    return function(scope, element, attr) {
        var fn = $parse(attr['ngBlur']);
        element.bind('blur', function(event) {
          scope.$apply(function() {
            fn(scope, {$event:event});
          });
        });
      }
}]).value("$swingModifiers" ,{
                      SHIFT_MASK : 1,
                      CTRL_MASK : 2,
                      META_MASK : 4,
                      ALT_MASK : 8,
                      ALT_GRAPH_MASK : 32,
                      BUTTON1_MASK : 16,
                      BUTTON2_MASK : 8,
                      META_MASK : 4,
                      SHIFT_DOWN_MASK : 64,
                      CTRL_DOWN_MASK : 128,
                      META_DOWN_MASK : 256,
                      ALT_DOWN_MASK : 512,
                      BUTTON1_DOWN_MASK : 1024,
                      BUTTON2_DOWN_MASK : 2048,
                      DOWN_MASK : 4096,
                      ALT_GRAPH_DOWN_MASK : 8192
}).value("$anchorConstants", {
                      NORTH : 1,
                      EAST : 2,
                      SOUTH : 4,
                      WEST : 8
}).factory("$utils",function($rootScope) {
	
	// internal function
	function getPropByStringPath(o, s) {
		s = s.replace(/\[(\w+)\]/g, '.$1'); // convert indexes to properties
		s = s.replace(/^\./, '');           // strip a leading dot
		var a = s.split('.');
		while (a.length) {
			var n = a.shift();
			if (n in o) {
				o = o[n];
			} else {
					return;
			}
			return o;
		}
	}
	
	return{
		
		/** this function can be used in filters .It accepts a string jsonpath the property to test for null. 
    	Example: "item in  model.valuelistID  | filter:notNull('realValue')"*/
		notNull : function (propPath){
			return function(item) {
				return !(getPropByStringPath(item,propPath) === null)
			}
		},
		watchProperty :  function ($scope,watchString,objectToPutChange, propertyStr){
				       $scope.$watch(watchString, function(newVal, oldVal) {
							 if (newVal) {
								 objectToPutChange[propertyStr] = newVal;
				       		 }
				       		 else {
				       		 	delete objectToPutChange[propertyStr];
				       		 }
				       })
		}
	}
}).value("$solutionSettings",  {
	mainForm: {},
	navigatorForm: {width:0},
	solutionTitle: "Servoy WebClient",
	defaultNavigatorState: {max:0,currentIdx:0,form:'<none>'}
}).controller("MainController", function($scope, $solutionSettings, $servoy) {
	$scope.solutionSettings = $solutionSettings;
}).factory("$windowService", function($modal, $log, $templateCache, $rootScope, $solutionSettings) {
	var instances = {};
	
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
		getContainerName: function(form) {
			for (var name in instances) {
				if (instances[name].form == form) {
					return name;
				}
			}
			return null;
		},
		switchForm: function(name,mainForm,navigatorForm,title) {		
        	$rootScope.$apply(function() { // TODO treat multiple windows case
        		if($solutionSettings.windowName == name) { // main window form switch
        			$solutionSettings.mainForm = mainForm;
        			$solutionSettings.navigatorForm = navigatorForm;
        			$solutionSettings.solutionTitle = title;
        		}
    		})
		},
	}
	
}).directive('modalWindow', ['$modalStack', '$timeout', function ($modalStack, $timeout) {
    return {
        restrict: 'EA',
        link: function (scope, element, attrs) {
          scope.$$childHead.formSize = scope.formSize; 
        }
      };
}]).controller("DialogInstanceCtrl", function ($scope, $modalInstance,$windowService, $servoy,windowName,title,form,formSize) {
	$scope.form = form;
	$scope.title = title;
	$scope.windowName = windowName;
	$scope.formSize =formSize;
	
	$servoy.setFormVisibility(form,true);
	
	$scope.cancel = function () {
		var promise = $servoy.callService("windowService", "windowClosing", {window:windowName});
		promise.then(function(ok) {
    		if (ok) {
    			$windowService.dismiss(windowName);
    			
    		}
    	})
	};
}).directive('svyBorder',  function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
        	if(attrs.svyBorder){
        	  scope.$watch(attrs.svyBorder,function(newVal){
        		  if(typeof newVal !== 'object') return;
        		  if(newVal.type == "TitledBorder"){
        			  element.wrap('<fieldset style="padding:5px;margin:0px;border:1px solid silver;width:100%;height:100%"></fieldset>')
        			  element.parent().prepend("<legend align='"+newVal.titleJustiffication+"' style='border-bottom:0px; margin:0px;width:auto;color:"+
        					  newVal.color+";font:"+newVal.font+"'>"+newVal.title+"</legend>")
        			  // TODO unwrap fieldset if borderType changes for example from TitledBorder to LineBorder
        		  }else if(newVal.borderStyle){ 
        			  element.css('border','')
        			  element.css(newVal.borderStyle)
        		  }
        	  })
        	}
        }
      };
})



