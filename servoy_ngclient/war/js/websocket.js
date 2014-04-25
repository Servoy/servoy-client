/**
 * Setup the webSocketModule.
 */
var webSocketModule = angular.module('webSocketModule', []);

/**
 * Setup the $webSocket service.
 */
webSocketModule.factory('$webSocket',
		function($rootScope, $injector, $log, $q) {

			var websocket = null

			var nextMessageId = 1

			var getNextMessageId = function() {
				return nextMessageId++
			}

			var deferredEvents = {};

			var convertServerObject = function(value, toType) {
				if (toType == "Date") {
					value = new Date(value);
				}
				return value;
			}

			var applyConversion = function(data, conversion) {
				for ( var conKey in conversion) {
					if (conversion[conKey] == "Date") {
						data[conKey] = convertServerObject(data[conKey],
								conversion[conKey]);
					} else {
						applyConversion(data[conKey], conversion[conKey]);
					}
				}
			}

			var handleMessage = function(wsSession, message) {
				var obj
				var responseValue
				try {
					obj = JSON.parse(message.data);
					if (obj.conversions) {
						applyConversion(obj, obj.conversions)
					}

					// data got back from the server
					if (obj.cmsgid) { // response to event
						if (obj.exception) {
							// something went wrong
							$rootScope.$apply(function() {
								deferredEvents[obj.cmsgid]
										.reject(obj.exception);
							})
						} else {
							$rootScope.$apply(function() {
								deferredEvents[obj.cmsgid].resolve(obj.ret);
							})
						}
						delete deferredEvents[obj.cmsgid];
					}

					if (obj.services) {
						// services call
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
						responseValue = wsSession.onMessageObject(obj.msg)
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

			var callService = function(serviceName, methodName, argsObject) {
				return sendDeferredMessage({
					service : serviceName,
					methodname : methodName,
					args : argsObject
				})
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
				this.onMessageObject = function(msg) {
					$log.error('Error: Received unexpected message: ' + msg);
				}

			};

			/**
			 * The $webSocket service API.
			 */
			return {

				connect : function(endpointType, id, argument) {

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
					new_uri += '/' + endpointType + '/' + (id || 'NULL') + '/'
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
		});
