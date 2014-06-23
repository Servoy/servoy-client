/**
 * Setup the webSocketModule.
 */
var webSocketModule = angular.module('webSocketModule', []);

/**
 * Setup the $webSocket service.
 */
webSocketModule.factory('$webSocket',
		function($rootScope, $injector, $log, $q, $services, $sabloConverters) {

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
						if (obj.exception) {
							// something went wrong
							if (obj.conversions && obj.conversions.exception) {
								$sabloConverters.convertFromServerToClient(obj.exception, obj.conversions.exception)
							}
							$rootScope.$apply(function() {
								deferredEvents[obj.cmsgid]
										.reject(obj.exception);
							})
						} else {
							if (obj.conversions && obj.conversions.ret) {
								$sabloConverters.convertFromServerToClient(obj.ret, obj.conversions.ret)
							}
							$rootScope.$apply(function() {
								deferredEvents[obj.cmsgid].resolve(obj.ret);
							})
						}
						delete deferredEvents[obj.cmsgid];
					}
					
					 if (obj.msg && obj.msg.services) {
						 $services.updateStates(obj.msg.services, obj.conversions ? obj.conversions.services : undefined);
			        }

					if (obj.services) {
						// services call
						if (obj.conversions && obj.conversions.services) {
							$sabloConverters.convertFromServerToClient(obj.services, obj.conversions.services)
						}
						for ( var index in obj.services) {
							var service = obj.services[index];
							var serviceInstance = $injector.get(service.name);
							if (serviceInstance
									&& serviceInstance[service.call]) {
								// responseValue keeps last services call return
								// value
								responseValue = serviceInstance[service.call]
										.apply(serviceInstance, service.args);
							}
						}
					}

					// message
					if (obj.msg && wsSession.onMessageObject) {
						responseValue = wsSession.onMessageObject(obj.msg, obj.conversions ? obj.conversions.msg : undefined)
					}

				} catch (e) {
					$log.error("error in parsing message: " + message.data);
					$log.error(e);
				} finally {
					if (obj && obj.smsgid) {
						// server wants a response
						var response = {
							smsgid : obj.smsgid
						}
						if (responseValue != undefined) {
							response.ret = convertClientObject(responseValue);
						}
						sendMessageObject(response);
					}
				}
			}

			var sendMessageObject = function(obj) {
				websocket.send(JSON.stringify(obj))
			}

			var sendDeferredMessage = function(obj) {
				// TODO: put cmsgid and obj in envelope
				var deferred = $q.defer();
				var cmsgid = getNextMessageId()
				deferredEvents[cmsgid] = deferred;
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

			var convertClientObject = function(value) {
				if (value instanceof Date) {
					value = value.getTime();
				}
				return value;
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
						if (wsSession.onopen)
							wsSession.onopen(evt)
					}
					websocket.onerror = function(evt) {
						if (wsSession.onerror)
							wsSession.onerror(evt)
					}
					websocket.onclose = function(evt) {
						if (wsSession.onclose)
							wsSession.onclose(evt)
					}
					websocket.onmessage = function(message) {
						handleMessage(wsSession, message)
					}

					return wsSession
				},

				convertClientObject : convertClientObject
			};
		}).factory("$services", function($rootScope, $sabloConverters){
			// serviceName:{} service model
			var serviceStates = {};
			var serviceStatesConversionInfo = {};
			return {
				getServiceState: function(serviceName) {
					if (!serviceStates[serviceName]) serviceStates[serviceName] = {};
		    		return serviceStates[serviceName];
				},
				updateStates: function(services, conversionInfo) {
					$rootScope.$apply(function() {
		        		 for(var servicename in services) {
		 		        	// current model
		 		            var serviceState = serviceStates[servicename];
		 		            if (!serviceState) {
		 		            	if (conversionInfo && conversionInfo[servicename]) {
	 		            			// convert all properties, remember type for when a client-server conversion will be needed
									$sabloConverters.convertFromServerToClient(services[servicename], conversionInfo[servicename], serviceStates[servicename])
		 		            		serviceStatesConversionInfo[servicename] = conversionInfo[servicename];
		 		            	}
		 		            	serviceStates[servicename] = services[servicename];
		 		            }
		 		            else {
		 		            	var serviceData = services[servicename];
		 		            	for(var key in serviceData) {
		 		            		if (conversionInfo && conversionInfo[servicename] && conversionInfo[servicename][key]) {
		 		            			// convert property, remember type for when a client-server conversion will be needed
		 		            			if (!serviceStatesConversionInfo[servicename]) serviceStatesConversionInfo[servicename] = {};
										$sabloConverters.convertFromServerToClient(serviceData[key], conversionInfo[servicename][key], serviceStates[servicename][key])
		 		            			serviceStatesConversionInfo[servicename][key] = conversionInfo[servicename][key];
		 		            		}
		 		            		serviceStates[servicename][key] = serviceData[key];
		 		             	}
		 		            }
		        		 }
		        	});
				}
			}
		}).factory("$sabloConverters", function($log) {
			/**
			 * Custom property converters can be registered via this service method: $webSocket.registerCustomPropertyHandler(...)
			 */
			var customPropertyConverters = {};

			var convertFromServerToClient = function(serverSentData, conversionInfo, currentClientData) {
				if (typeof conversionInfo === 'string' || typeof conversionInfo === 'number') {
					var customConverter = customPropertyConverters[conversionInfo];
					if (customConverter) serverSentData = customConverter.fromServerToClient(serverSentData, currentClientData);
					else { //converter not found - will not convert
						$log.error("cannot find type converter (s->c) for: '" + conversionInfo + "'.");
					}
				} else if (conversionInfo) {
					for (var conKey in conversionInfo) {
						serverSentData[conKey] = convertFromServerToClient(serverSentData[conKey], conversionInfo[conKey], currentClientData ? currentClientData[conKey] : undefined);
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
				 *				// @return the new/updated client side property value; if this returned value is interested in triggering
				 *				//         updates to server when something changes client side it must have these member functions:
				 *				//				setChangeNotifier: function(changeNotifier) - where changeNotifier is a function that can be called when
				 *				//                                                          the value needs to send updates to the server; this method will
				 *				//                                                          not be called when value is a call parameter for example, but will
				 *				//                                                          be called when set into a component's/service's property/model
				 *				//              isChanged: function() - should return true if the value needs to send updates to server // TODO this could be kept track of internally
				 * 				fromServerToClient: function (serverSentJSONValue, currentClientValue) { (...); return newClientValue; },
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
		});
