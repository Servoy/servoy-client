/// <reference path="../../../typings/angularjs/angular.d.ts" />
/// <reference path="../../../typings/sablo/sablo.d.ts" />

angular.module('valuelist_property', ['webSocketModule'])
// Valuelist type -------------------------------------------
.run(function ($sabloConverters, $sabloUtils, $q, $sabloApplication, $sabloDeferHelper: sablo.ISabloDeferHelper) {
	var ID_KEY = "id";
	var VALUE_KEY = "value";
	var HANDLED = "handledID";
	var FILTER = "filter";
	var DISPLAYVALUE = "getDisplayValue";

	$sabloConverters.registerCustomPropertyHandler('valuelist', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, propertyContext) {
			var newValue;
			if (serverJSONValue) {
				var deferredValue;
				var internalState;
				if ( serverJSONValue.values) {
					newValue = serverJSONValue.values;
					deferredValue = newValue;
					
					// because we reuse directly what we get from server serverJSONValue.values and because valuelists can be foundset linked (forFoundset in .spec) but not actually bound to records (for example custom valuelist),
					// it is possible that foundsetLinked.js generates the whole viewport of the foundset using the same value comming from the server => this conversion will be called multiple times
					// with the same serverJSONValue so serverJSONValue.values might already be initialized... so skip it then
					if (!newValue[$sabloConverters.INTERNAL_IMPL]) {
						// initialize
						$sabloConverters.prepareInternalState(newValue);
						internalState = newValue[$sabloConverters.INTERNAL_IMPL]; // internal state / $sabloConverters interface
	
						if (currentClientValue && currentClientValue[$sabloConverters.INTERNAL_IMPL]) $sabloDeferHelper.initInternalStateForDeferringFromOldInternalState(internalState, currentClientValue[$sabloConverters.INTERNAL_IMPL]);
						else $sabloDeferHelper.initInternalStateForDeferring(internalState, "svy valuelist * ");
	
						internalState.hasRealValues = serverJSONValue.hasRealValues; 
						// PUBLIC API to components; initialize the property value; make it 'smart'
						Object.defineProperty(newValue, 'filterList', {
							value: function(filterString) {
								// only block once
								internalState.filterStringReq = { };
								internalState.filterStringReq[FILTER] = filterString;
								internalState.filterStringReq[ID_KEY] = $sabloDeferHelper.getNewDeferId(internalState);
								var promise = internalState.deferred[internalState.filterStringReq[ID_KEY]].defer.promise;
								
								if (internalState.changeNotifier) internalState.changeNotifier();
								
								return promise;
							}, enumerable: false });
	
						Object.defineProperty(newValue, 'hasRealValues', {
							value: function() {
								return internalState.hasRealValues;
							}, enumerable: false });
						// clear the cache
						internalState.realToDisplayCache = {};
	
						Object.defineProperty(newValue, 'getDisplayValue', {
							value: function(realValue) {
								if (realValue != null && realValue != undefined) {
									if (internalState.valuelistid == undefined) {
										return { then: function(then) { then(realValue) } };
									} else {
										var key = realValue + '';
										if (internalState.realToDisplayCache[key] !== undefined) {
											// if this is a promise return that.
											if (internalState.realToDisplayCache[key] && angular.isFunction(internalState.realToDisplayCache[key].then)) return internalState.realToDisplayCache[key];
											// if the value is in the cache then return a promise like object
											// that has a then function that will be resolved right away when called. So that it is more synch api.
											return { then: function(then) { then(internalState.realToDisplayCache[key]) } }
										}
										internalState.diplayValueReq = { };
										internalState.diplayValueReq[DISPLAYVALUE] = realValue;
										internalState.diplayValueReq[ID_KEY] = $sabloDeferHelper.getNewDeferId(internalState);
										internalState.realToDisplayCache[key] = internalState.deferred[internalState.diplayValueReq[ID_KEY]].defer.promise.then(function(val) {
											internalState.realToDisplayCache[key] = val;
											return val;
										});
										
										if (internalState.changeNotifier) internalState.changeNotifier();
										
										return internalState.realToDisplayCache[key];
									}
								}
								// the real value == null return a promise like function so that not constantly promises are made.
								return { then: function(then) { then("") } }
							}, enumerable: false });
						
						internalState.valuelistid = serverJSONValue.valuelistid; 
						// PRIVATE STATE AND IMPL for $sabloConverters (so something components shouldn't use)
						// $sabloConverters setup
						internalState.setChangeNotifier = function(changeNotifier) {
							internalState.changeNotifier = changeNotifier; 
						}
						internalState.isChanged = function() { return angular.isDefined(internalState.filterStringReq) || angular.isDefined(internalState.diplayValueReq); }
					} else internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				}
				else if (serverJSONValue[DISPLAYVALUE] !== undefined) {
					// this is the GETDISPLAYVALUE
					newValue = currentClientValue;
					internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];
					deferredValue = serverJSONValue[DISPLAYVALUE];
				}
				
				// if we have a deferred filter request, notify that the new value has arrived
				if (angular.isDefined(serverJSONValue[HANDLED])) {
					var handledIDAndState = serverJSONValue[HANDLED]; // { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
				
					var defer = $sabloDeferHelper.retrieveDeferForHandling(handledIDAndState[ID_KEY], internalState);
					if (defer) {
						if (handledIDAndState[VALUE_KEY]) defer.resolve(deferredValue);
						else defer.reject();
					}
				}
			} else {
				newValue = null;
				var oldInternalState = currentClientValue ? currentClientValue[$sabloConverters.INTERNAL_IMPL] : undefined; // internal state / $sabloConverters interface
				if (oldInternalState) $sabloDeferHelper.cancelAll(oldInternalState);
			}
			
			return newValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData) {
				var newDataInternalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (newDataInternalState.isChanged()) {
					if (newDataInternalState.filterStringReq) {
						var tmp = newDataInternalState.filterStringReq;
						delete newDataInternalState.filterStringReq;
						return tmp;
					}
					else if (newDataInternalState.diplayValueReq) {
						var tmp = newDataInternalState.diplayValueReq;
						delete newDataInternalState.diplayValueReq;
						return tmp;
					}
				}
			}
			return null; // should never happen
		},
		
		updateAngularScope: function(clientValue, componentScope) {
			// nothing to do here
		}
		
	});
})
