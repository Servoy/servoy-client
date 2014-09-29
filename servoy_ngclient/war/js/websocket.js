/**
 * Setup the webSocketModule.
 */
var webSocketModule = angular.module('webSocketModule', []);

/**
 * Setup the $webSocket service.
 */
webSocketModule.factory('$webSocket',
		function($rootScope, $injector, $log, $q, $services, $sabloConverters, $sabloUtils) {

			var websocket = null

			var nextMessageId = 1

			var getNextMessageId = function() {
				return nextMessageId++
			}

			var deferredEvents = {};
			
			var handleMessage = function(wsSession, message) {
				var obj
				var responseValue
				try {
					obj = JSON.parse(message.data);

					// data got back from the server
					if (obj.cmsgid) { // response to event
						var deferredEvent = deferredEvents[obj.cmsgid];
						if (obj.exception) {
							// something went wrong
							if (obj.conversions && obj.conversions.exception) {
								obj.exception = $sabloConverters.convertFromServerToClient(obj.exception, obj.conversions.exception, undefined, undefined)
							}
							if (deferredEvent.scope) {
								deferredEvent.deferred.reject(obj.exception);
								deferredEvent.scope.$digest();
							}
							else {
								$rootScope.$apply(function() {
									deferredEvent.deferred.reject(obj.exception);
								})
							}
						} else {
							if (obj.conversions && obj.conversions.ret) {
								obj.ret = $sabloConverters.convertFromServerToClient(obj.ret, obj.conversions.ret, undefined, undefined)
							}
							if (deferredEvent.scope) {
								deferredEvent.deferred.resolve(obj.ret);
								deferredEvent.scope.$digest();
							}
							else {
								$rootScope.$apply(function() {
									deferredEvent.deferred.resolve(obj.ret);
								})
							}
						}
						delete deferredEvents[obj.cmsgid];
					}

					if (obj.msg && obj.msg.services) {
						$services.updateServiceScopes(obj.msg.services, (obj.conversions && obj.conversions.msg) ? obj.conversions.msg.services : undefined);
					}

					if (obj.services) {
						// services call
						if (obj.conversions && obj.conversions.services) {
							obj.services = $sabloConverters.convertFromServerToClient(obj.services, obj.conversions.services, undefined, undefined)
						}
						for (var index in obj.services) {
							var service = obj.services[index];
							var serviceInstance = $injector.get(service.name);
							if (serviceInstance
									&& serviceInstance[service.call]) {
								// responseValue keeps last services call return value
								responseValue = serviceInstance[service.call].apply(serviceInstance, service.args);
								$services.digest(service.name);
							}
						}
					}

					// message
					if (obj.msg && wsSession.onMessageObject) {
						responseValue = wsSession.onMessageObject(obj.msg, obj.conversions ? obj.conversions.msg : undefined)
					}

				} catch (e) {
					$log.error("error (follows below) in parsing/processing message: " + message.data);
					$log.error(e);
				} finally {
					if (obj && obj.smsgid) {
						// server wants a response
						var response = {
							smsgid : obj.smsgid
						}
						if (responseValue != undefined) {
							response.ret = $sabloUtils.convertClientObject(responseValue);
						}
						sendMessageObject(response);
					}
				}
			}

			var sendMessageObject = function(obj) {
				websocket.send(JSON.stringify(obj))
			}

			var sendDeferredMessage = function(obj,scope) {
				// TODO: put cmsgid and obj in envelope
				var deferred = $q.defer();
				var cmsgid = getNextMessageId()
				deferredEvents[cmsgid] = {deferred:deferred,scope:scope}
				var cmd = obj || {}
				cmd.cmsgid = cmsgid
				sendMessageObject(cmd)
				return deferred.promise;
			}

			var callService = function(serviceName, methodName, argsObject,async) {
				var cmd = {
						service : serviceName,
						methodname : methodName,
						args : argsObject
					};
				if (async)
				{
					sendMessageObject(cmd);
				}
				else
				{
					return sendDeferredMessage(cmd)
				}
			}

			var WebsocketSession = function() {

				// api
				this.sendMessageObject = sendMessageObject

				this.sendDeferredMessage = sendDeferredMessage

				this.callService = callService

				// some default handlers, can be overwritten
				this.onopen = function() {
					$log.info('Info: WebSocket connection opened.');
				}
				this.onerror = function(message) {
					$log.error('Error: WebSocket on error: ' + message);
				}
				this.onclose = function(message) {
					$log.info('Info: WebSocket on close, code: ' + message.code
							+ ' , reason: ' + message.reason);
				}
				// This one should be overwritten if you expect other messages
				// then service calls
				this.onMessageObject = function(msg, conversionInfo) {
					$log.error('Error: Received unexpected message: (-' + msg + '-,\n-' + conversionInfo + '-)');
				}

			};
			
			var connected = false;

			/**
			 * The $webSocket service API.
			 */
			return {

				connect : function(endpointType, sessionid,windowid, argument) {

					var loc = window.location, new_uri;
					if (loc.protocol === "https:") {
						new_uri = "wss:";
					} else {
						new_uri = "ws:";
					}
					new_uri += "//" + loc.host;
					var lastIndex = loc.pathname.lastIndexOf("/");
					if (lastIndex > 0) {
						new_uri += loc.pathname.substring(0, lastIndex)
								+ "/../../websocket";
					} else {
						new_uri += loc.pathname + "/websocket";
					}
					new_uri += '/' + endpointType + '/' + (sessionid || 'NULL') + '/' + (windowid || 'NULL') + '/'
							+ (argument || 'NULL')

					websocket = new WebSocket(new_uri);

					var wsSession = new WebsocketSession()
					websocket.onopen = function(evt) {
						$rootScope.$apply(function() {
							connected = true;
						})
						if (wsSession.onopen)
							wsSession.onopen(evt)
					}
					websocket.onerror = function(evt) {
						if (wsSession.onerror)
							wsSession.onerror(evt)
					}
					websocket.onclose = function(evt) {
						$rootScope.$apply(function() {
							connected = false;
						})
						if (wsSession.onclose)
							wsSession.onclose(evt)
					}
					websocket.onmessage = function(message) {
						handleMessage(wsSession, message)
					}
					
					// todo should we just merge $websocket and $services into $sablo that just has all
					// the public api of sablo (like connect, conversions, services)
					$services.setSession(wsSession);

					return wsSession
				},
				
				isConnected: function() {
					return connected;
				}
			};
		}).factory("$services", function($rootScope, $sabloConverters, $sabloUtils){
			// serviceName:{} service model
			var serviceScopes = $rootScope.$new(true);
			var serviceScopesConversionInfo = {};
			var watches = {}
			var wsSession = null;
			var sendServiceChanges = function(now, prev, servicename) {
				   // first build up a list of all the properties both have.
				   var fulllist = $sabloUtils.getCombinedPropertyNames(now,prev);
				   var conversionInfo = serviceScopesConversionInfo[servicename];
				   var changes = {}, prop;

				   for (prop in fulllist) {
					   var changed = false;
					   if (!prev) {
						   changed = true;
					   }
					   else if (prev[prop] !== now[prop]) {
						   if (typeof now[prop] == "object") {
							   if ($sabloUtils.isChanged(now[prop], prev[prop], conversionInfo ? conversionInfo[prop] : undefined)) {
								   changed = true;
							   }
						   } else {
							   changed = true;
						   }
					   }
					   if (changed) {
						   if (conversionInfo && conversionInfo[prop]) changes[prop] = $sabloConverters.convertFromClientToServer(now[prop], conversionInfo[prop], prev ? prev[prop] : undefined);
						   else changes[prop] = $sabloUtils.convertClientObject(now[prop])
					   }
				   }
				   for (prop in changes) {
					   wsSession.sendMessageObject({servicedatapush:servicename,changes:changes})
					   return;
				   }
			};
			var getChangeNotifier = function(servicename) {
				return function() {
					var serviceModel = serviceScopes[servicename];
					sendServiceChanges(serviceModel, serviceModel, servicename);
				}
			};
			var watch = function(servicename) {
				return function(newVal, oldVal) {
					if (newVal === oldVal) return;
					sendServiceChanges(newVal,oldVal,servicename);
				}
			};
			return {
				getServiceScope: function(serviceName) {
					if (!serviceScopes[serviceName]) {
						serviceScopes[serviceName] = serviceScopes.$new(true);
						serviceScopes[serviceName].model = {};
						watches[serviceName] = serviceScopes[serviceName].$watch("model",watch(serviceName),true);
					}
		    		return serviceScopes[serviceName];
				},
				updateServiceScopes: function(services, conversionInfo) {
	        		 for(var servicename in services) {
	 		        	// current model
	 		            var serviceScope = serviceScopes[servicename];
	 		            if (!serviceScope) {
	 		            	serviceScopes[serviceName] = serviceScopes.$new(true);
	 		            	// so no previous service state; set it now
	 		            	if (conversionInfo && conversionInfo[servicename]) {
 		            			// convert all properties, remember type for when a client-server conversion will be needed
	 		            		services[servicename] = $sabloConverters.convertFromServerToClient(services[servicename], conversionInfo[servicename], undefined, serviceScopes[serviceName])
	 		            		var changeNotifier = getChangeNotifier(servicename);
	 		            		for (var pn in conversionInfo[servicename]) {
	 		            			if (services[servicename][pn] && services[servicename][pn][$sabloConverters.INTERNAL_IMPL]
	 		            					&& services[servicename][pn][$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
	 		            				services[servicename][pn][$sabloConverters.INTERNAL_IMPL].setChangeNotifier(changeNotifier);
	 		            			}
	 		            		}
	 		            		serviceScopesConversionInfo[servicename] = conversionInfo[servicename];
	 		            	}
	 		            	serviceScopes[servicename].model = services[servicename];
	 		            }
	 		            else {
	 		            	var serviceData = services[servicename];
	 		            	// unregister the watch.
	 		            	watches[servicename]();
 		            		var changeNotifier = (conversionInfo && conversionInfo[servicename]) ? getChangeNotifier(servicename) : undefined;

	 		            	for(var key in serviceData) {
	 		            		if (conversionInfo && conversionInfo[servicename] && conversionInfo[servicename][key]) {
	 		            			// convert property, remember type for when a client-server conversion will be needed
	 		            			if (!serviceScopesConversionInfo[servicename]) serviceScopesConversionInfo[servicename] = {};
	 		            			serviceData[key] = $sabloConverters.convertFromServerToClient(serviceData[key], conversionInfo[servicename][key], serviceScope.model[key], serviceScope)
	 		            			
	 		            			if ((serviceData[key] !== serviceScope.model[key] || serviceScopesConversionInfo[servicename][key] !== conversionInfo[servicename][key]) && serviceData[key]
	 		            					&& serviceData[key][$sabloConverters.INTERNAL_IMPL] && serviceData[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
	 		            				serviceData[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier(changeNotifier);
	 		            			}
	 		            			serviceScopesConversionInfo[servicename][key] = conversionInfo[servicename][key];
	 		            		}
	 		            		serviceScope.model[key] = serviceData[key];
	 		             	}
	 		            }
	 		            // register a new watch
	 		            watches[servicename] = serviceScopes[servicename].$watch("model",watch(servicename),true);
	 		            serviceScopes[servicename].$digest();
	        		 }
				},
				digest: function(servicename) {
					if (serviceScopes[servicename]) serviceScopes[servicename].$digest();
				},
				setSession: function(session) {
					wsSession = session;
				}
			}
		}).factory("$sabloConverters", function($log) {
			/**
			 * Custom property converters can be registered via this service method: $webSocket.registerCustomPropertyHandler(...)
			 */
			var customPropertyConverters = {};

			var convertFromServerToClient = function(serverSentData, conversionInfo, currentClientData, componentScope) {
				if (typeof conversionInfo === 'string' || typeof conversionInfo === 'number') {
					var customConverter = customPropertyConverters[conversionInfo];
					if (customConverter) serverSentData = customConverter.fromServerToClient(serverSentData, currentClientData, componentScope);
					else { //converter not found - will not convert
						$log.error("cannot find type converter (s->c) for: '" + conversionInfo + "'.");
					}
				} else if (conversionInfo) {
					for (var conKey in conversionInfo) {
						serverSentData[conKey] = convertFromServerToClient(serverSentData[conKey], conversionInfo[conKey], currentClientData ? currentClientData[conKey] : undefined, componentScope); // TODO should componentScope really stay the same here? 
					}
				}
				return serverSentData;
			};
			
			// converts from a client property JS value to a JSON that can be sent to the server using the appropriate registered handler
			var convertFromClientToServer = function(newClientData, conversionInfo, oldClientData) {
				if (typeof conversionInfo === 'string' || typeof conversionInfo === 'number') {
					var customConverter = customPropertyConverters[conversionInfo];
					if (customConverter) return customConverter.fromClientToServer(newClientData, oldClientData);
					else { //converter not found - will not convert
						$log.error("cannot find type converter (c->s) for: '" + conversionInfo + "'.");
						return newClientData;
					}
				} else if (conversionInfo) {
					var retVal = (Array.isArray ? Array.isArray(newClientData) : $.isArray(newClientData)) ? [] : {};
					for (var conKey in conversionInfo) {
						retVal[conKey] = convertFromClientToServer(newClientData[conKey], conversionInfo[conKey], oldClientData ? oldClientData[conKey] : undefined);
					}
					return retVal;
				} else {
					return newClientData;
				}
			};
			
			return {
				
				/**
				 * In a custom property value, the val[$sabloConverters.INTERNAL_IMPL] is to be used for internal state/impl details only - not to be accessed by components
				 */
				INTERNAL_IMPL: '__internalState',
				
				prepareInternalState: function(propertyValue) {
					if (Object.defineProperty) {
						// try to avoid unwanted iteration/non-intended interference over the private property state
						Object.defineProperty(propertyValue, this.INTERNAL_IMPL, {
							configurable: false,
							enumerable: false,
							writable: false,
							value: {}
						});
					} else propertyValue[$sabloConverters.INTERNAL_IMPL] = {};
				},
				
				convertFromServerToClient: convertFromServerToClient,
				
				convertFromClientToServer: convertFromClientToServer,
				
				/**
				 * Registers a custom client side property handler into the system. These handlers are useful
				 * for custom property types that require some special handling when received through JSON from server-side
				 * or for sending content updates back. (for example convert received JSON into a different JS object structure that will be used
				 * by beans or just implement partial updates for more complex property contents)
				 *  
				 * @param customHandler an object with the following methods/fields:
				 * {
				 * 
				 *				// Called when a JSON update is received from the server for a property
				 *				// @param serverSentJSONValue the JSON value received from the server for the property
				 *				// @param currentClientValue the JS value that is currently used for that property in the client; can be null/undefined if
				 *				//        conversion happens for service API call parameters for example...
				 *				// @param componentScope scope that can be used to add component and property related watches; can be null/undefined if
				 *				//        conversion happens for service API call parameters for example...
				 *				// @return the new/updated client side property value; if this returned value is interested in triggering
				 *				//         updates to server when something changes client side it must have these member functions in this[$sabloConverters.INTERNAL_IMPL]:
				 *				//				setChangeNotifier: function(changeNotifier) - where changeNotifier is a function that can be called when
				 *				//                                                          the value needs to send updates to the server; this method will
				 *				//                                                          not be called when value is a call parameter for example, but will
				 *				//                                                          be called when set into a component's/service's property/model
				 *				//              isChanged: function() - should return true if the value needs to send updates to server // TODO this could be kept track of internally
				 * 				fromServerToClient: function (serverSentJSONValue, currentClientValue, componentScope) { (...); return newClientValue; },
				 * 
				 *				// Converts from a client property JS value to a JSON that will be sent to the server.
				 *				// @param newClientData the new JS client side property value
				 *				// @param oldClientData the old JS JS client side property value; can be null/undefined if
				 *				//        conversion happens for service API call parameters for example...
				 *				// @return the JSON value to send to the server.
				 *				fromClientToServer: function(newClientData, oldClientData) { (...); return sendToServerJSON; }
				 * 
				 * }
				 */
				registerCustomPropertyHandler : function(propertyTypeID, customHandler) {
					customPropertyConverters[propertyTypeID] = customHandler;
				}
				
			};
		}).factory("$sabloUtils", function($log, $sabloConverters) { 
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
			 
			var isChanged = function(now, prev, conversionInfo) {
				   if ((typeof conversionInfo === 'string' || typeof conversionInfo === 'number') && now && now[$sabloConverters.INTERNAL_IMPL] && now[$sabloConverters.INTERNAL_IMPL].isChanged) {
					   return now[$sabloConverters.INTERNAL_IMPL].isChanged();
				   }
				   
				   if (now && prev) {
					   if (now instanceof Array) {
						   if (prev instanceof Array) {
							   if (now.length != prev.length) return true;
						   } else {
							   return true;
						   }
					   }
					   if (now instanceof Date) {
						   if (prev instanceof Date) {
							   return now.getTime() != prev.getTime();
						   }
						   return true;
					   }
					   if (now instanceof Object && !(prev instanceof Object)) return true;
					   // first build up a list of all the properties both have.
			    	   var fulllist = getCombinedPropertyNames(now,prev);
			    	    for (var prop in fulllist) {
		                    if(prop == "$$hashKey") continue; // ng repeat creates a child scope for each element in the array any scope has a $$hashKey property which must be ignored since it is not part of the model
			    	    	if (prev[prop] !== now[prop]) {
			    	    		if (typeof now[prop] == "object") {
			    	    			if (isChanged(now[prop],prev[prop], conversionInfo ? conversionInfo[prop] : undefined)) {
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
				isChanged: isChanged,
				getCombinedPropertyNames: getCombinedPropertyNames,
				convertClientObject : function(value) {
					if (value instanceof Date) {
						value = value.getTime();
					}
					return value;
				},
			}
		});
