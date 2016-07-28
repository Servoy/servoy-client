angular.module('valuelist_property', ['webSocketModule'])
// Valuelist type -------------------------------------------
.run(function ($sabloConverters, $sabloUtils, $q, $sabloTestability) {
	$sabloConverters.registerCustomPropertyHandler('valuelist', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			if (currentClientValue && angular.isDefined(currentClientValue[$sabloConverters.INTERNAL_IMPL].deferredFilter) &&
					(!serverJSONValue || serverJSONValue.filter != currentClientValue[$sabloConverters.INTERNAL_IMPL].savedFilterStringReq)) 
			{
				// if this is not the request for the latest filter request, ignore it and return the current client value (if for example multiple quick filter
				// requests were sent to server we are only interested in the response for the last filter call; the promises for the rest are already canceled see 'filterList' below)
				return currentClientValue;
			}
			
			var newValue;
			if (serverJSONValue) {
				newValue = serverJSONValue.values;
				$sabloConverters.prepareInternalState(newValue);
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL]; // internal state / $sabloConverters interface

				// PUBLIC API to components; initialize the property value; make it 'smart'
				Object.defineProperty(newValue, 'filterList', {
					value: function(filterString) {
						// only block once
						if (!angular.isDefined(internalState.deferredFilter)) {
							$sabloTestability.block(true);
						} else internalState.deferredFilter.reject("Previous filter canceled due to new one");
						
						var deferred = $q.defer();
						internalState.deferredFilter = deferred;

						internalState.filterStringReq = filterString;
						internalState.savedFilterStringReq = filterString;
						if (internalState.changeNotifier) internalState.changeNotifier();
						
						return deferred.promise;
					}, enumerable: false });
				if (serverJSONValue.valuelistid)
				{
					Object.defineProperty(newValue, 'valuelistid', {value: serverJSONValue.valuelistid , enumerable: false });
				}	
				// PRIVATE STATE AND IMPL for $sabloConverters (so something components shouldn't use)
				// $sabloConverters setup
				internalState.setChangeNotifier = function(changeNotifier) {
					internalState.changeNotifier = changeNotifier; 
				}
				internalState.isChanged = function() { return angular.isDefined(internalState.filterStringReq); }
			} else newValue = null;
			
			// if we have a deferred filter request, notify that the new value has arrived
			if (currentClientValue && angular.isDefined(currentClientValue[$sabloConverters.INTERNAL_IMPL].deferredFilter)) {
				currentClientValue[$sabloConverters.INTERNAL_IMPL].deferredFilter.resolve(newValue);
				$sabloTestability.block(false);
				
				// cleanup; these following 2 lines are not actually needed because we return a new reference here so the old one will not be used anymore
				delete currentClientValue[$sabloConverters.INTERNAL_IMPL].deferredFilter; 
				delete currentClientValue[$sabloConverters.INTERNAL_IMPL].savedFilterStringReq;
			}
			
			return newValue;
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
