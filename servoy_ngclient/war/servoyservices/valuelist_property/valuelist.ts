/// <reference path="../../../typings/angularjs/angular.d.ts" />
/// <reference path="../../../typings/sablo/sablo.d.ts" />

angular.module('valuelist_property', ['webSocketModule'])
// Valuelist type -------------------------------------------
.run(function ($sabloConverters: sablo.ISabloConverters, $sabloDeferHelper: sablo.ISabloDeferHelper, $typesRegistry: sablo.ITypesRegistry) {
	var ID_KEY = "id";
	var VALUE_KEY = "value";
	var HANDLED = "handledID";
	var FILTER = "filter";
	var DISPLAYVALUE = "getDisplayValue";

	$typesRegistry.registerGlobalType('valuelist', {
		fromServerToClient: function (serverJSONValue: any, currentClientValue: any, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
			// TODO it seems that the valuelist type server side never sends type info, just a "...dates" boolean that is used only in NG2 probably not be break things in NG1
			// (it either writes default to JSON a value in case of a response to 'getDisplayValue' or a full valuelist
			// generated as java array/map with default to JSON conversion); but it always changes Date to String in
			// it's impl. before applying default to JSON conversions so no client side type info is needed then; so client side here as well it will not apply
			// any server-to-client conversion on values, not even for dates - because they are already strings... changing it so that it works properly with
			// Date conversions to generate actual Date values for entries might break existing components / formatters? though because they now expect strings instead of dates
			
			var newValue;
			if (serverJSONValue) {
				var deferredValue;
				var internalState;
				if (serverJSONValue.values) {
					newValue = serverJSONValue.values;
					deferredValue = newValue;
					
					// because we reuse directly what we get from server serverJSONValue.values and because valuelists can
					// be foundset linked (forFoundset in .spec) but not actually bound to records (for example custom valuelist),
					// it is possible that foundsetLinked.js generates the whole viewport of the foundset using the same value coming from
					// the server => this conversion will be called multiple times
					// with the same serverJSONValue so serverJSONValue.values might already be initialized... so skip it then
					if (!newValue[$sabloConverters.INTERNAL_IMPL]) {
						// initialize
						$sabloConverters.prepareInternalState(newValue);
						internalState = newValue[$sabloConverters.INTERNAL_IMPL]; // internal state / $sabloConverters interface
	
						if (currentClientValue && currentClientValue[$sabloConverters.INTERNAL_IMPL]) $sabloDeferHelper.initInternalStateForDeferringFromOldInternalState(internalState, currentClientValue[$sabloConverters.INTERNAL_IMPL]);
						else $sabloDeferHelper.initInternalStateForDeferring(internalState, "svy valuelist * ");
	
						internalState.hasRealValues = serverJSONValue.hasRealValues; 
                        internalState.realValueType = serverJSONValue.realValueType;
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
                        Object.defineProperty(newValue, 'isRealValueUUID', {
                            value: function() {
                                return internalState.realValueType == 'UUID';
                            }, enumerable: false });
						// clear the cache
						internalState.realToDisplayCache = {};
						if (componentScope) {
                            if (internalState.destroyDeregistener) {
                                internalState.destroyDeregistener();
                            }
    						internalState.destroyDeregistener  = componentScope.$on('$destroy', () =>{
                                $sabloDeferHelper.cancelAll(internalState);
                                delete internalState.destroyDeregistener;
                             }  );
    	                }
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
										}).catch(() => {
                                           delete internalState.realToDisplayCache[key];
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

		fromClientToServer: function (newClientData: any, oldClientData: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
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
            if (clientValue){
                const internalState = clientValue[$sabloConverters.INTERNAL_IMPL];
                if (internalState && componentScope) {
                                if (internalState.destroyDeregistener) {
                                    internalState.destroyDeregistener();
                                }
                                internalState.destroyDeregistener  = componentScope.$on('$destroy', () =>{
                                    $sabloDeferHelper.cancelAll(internalState);
                                    delete internalState.destroyDeregistener;
                                 }  );
                }  
    		}
		}
		
	}, false);
})
