angular.module('valuelist_property', ['webSocketModule'])
// Valuelist type -------------------------------------------
.run(function ($sabloConverters, $sabloUtils, $q, $sabloTestability) {
	$sabloConverters.registerCustomPropertyHandler('valuelist', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			var realServerJSONValue = serverJSONValue;
			// if we have a deferred filter, notify that the new value arrived
			if (currentClientValue && angular.isDefined(currentClientValue[$sabloConverters.INTERNAL_IMPL].deferredFilter)) {
				if (realServerJSONValue && realServerJSONValue.filter != currentClientValue[$sabloConverters.INTERNAL_IMPL].savedFilterStringReq) 
				{
					// if this is not the request for the latest filter request, ignore and retourn the current client value.
					return currentClientValue;
				}
				componentScope.$evalAsync(function() {
					$sabloTestability.block(false);
					currentClientValue[$sabloConverters.INTERNAL_IMPL].deferredFilter.resolve(realServerJSONValue.values);
					delete currentClientValue[$sabloConverters.INTERNAL_IMPL].deferredFilter; // this isn't actually needed cause the old value isn't used anymore
					delete currentClientValue[$sabloConverters.INTERNAL_IMPL].savedFilterStringReq; // this isn't actually needed cause the old value isn't used anymore
				});
			}
			
			if (serverJSONValue) {
					serverJSONValue = serverJSONValue.values;
					$sabloConverters.prepareInternalState(serverJSONValue);
					var internalState = serverJSONValue[$sabloConverters.INTERNAL_IMPL]; // internal state / $sabloConverters interface
					
					// PUBLIC API to components; initialize the property value; make it 'smart'
					Object.defineProperty(serverJSONValue, 'filterList',
					{
						value:  function(filterString) {
							var retVal = serverJSONValue;

							// only block once
							if(!angular.isDefined(internalState.deferredFilter)) {
								$sabloTestability.block(true);
							}
							else internalState.deferredFilter.reject("Previous filter canceled due to new one");
							var deferred = $q.defer();
							internalState.deferredFilter = deferred;
							retVal = deferred.promise;

							internalState.filterStringReq = filterString;
							internalState.savedFilterStringReq = filterString;
							if (internalState.changeNotifier) internalState.changeNotifier();
							return retVal;
						},
						enumerable: false
					});
					
					// PRIVATE STATE AND IMPL for $sabloConverters (so something components shouldn't use)
					// $sabloConverters setup
					internalState.setChangeNotifier = function(changeNotifier) {
						internalState.changeNotifier = changeNotifier; 
					}
					internalState.isChanged = function() { return angular.isDefined(internalState.filterStringReq); }
			}		 
			return serverJSONValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData) {
				var newDataInternalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (newDataInternalState.isChanged()) {
					var tmp = newDataInternalState.filterStringReq;
					delete newDataInternalState.filterStringReq;
					return tmp;
				}
			}
			return null; // should never happen
		},
		
		updateAngularScope: function(clientValue, componentScope) {
			// nothing to do here
		}
		
	});
})
