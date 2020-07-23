angular.module('foundset_linked_property', ['webSocketModule', 'servoyApp', 'foundset_custom_property', 'foundset_viewport_module'])
// Foundset linked type ------------------------------------------
.value("$foundsetLinkedTypeConstants", {
	ID_FOR_FOUNDSET: "idForFoundset",
	RECORD_LINKED: "recordLinked"
})
.run(function($sabloConverters: sablo.ISabloConverters, $sabloUtils: sablo.ISabloUtils, $viewportModule: ngclient.propertyTypes.ViewportService,
		$log: sablo.ILogService, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants, $foundsetLinkedTypeConstants,
		$webSocket: sablo.IWebSocket, $typesRegistry: sablo.ITypesRegistry) {

	$typesRegistry.registerGlobalType('fsLinked', new ngclient.propertyTypes.FoundsetLinkedType($sabloConverters, $sabloUtils,
			$viewportModule, $log, $foundsetTypeConstants, $foundsetLinkedTypeConstants,
			$webSocket));
});

namespace ngclient.propertyTypes {

	export class FoundsetLinkedType implements sablo.IType<FoundsetLinkedValue> {
		
		static readonly SINGLE_VALUE = "sv";
		static readonly SINGLE_VALUE_UPDATE = "svu";
		static readonly VIEWPORT_VALUE = "vp";
		static readonly VIEWPORT_VALUE_UPDATE = "vpu";
		static readonly PROPERTY_CHANGE = "propertyChange";

		static readonly PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched

		constructor(private readonly sabloConverters: sablo.ISabloConverters, private readonly sabloUtils: sablo.ISabloUtils,
				private readonly viewportModule: ngclient.propertyTypes.ViewportService,
				private readonly log: sablo.ILogService,
				private readonly foundsetTypeConstants: foundsetType.FoundsetTypeConstants, private readonly foundsetLinkedTypeConstants,
				private readonly webSocket: sablo.IWebSocket) {}
		
		private getUpdateWholeViewportFunc(propertyContext: sablo.IPropertyContext) {
			return (propValue: any[], internalState, wholeViewport: any[], conversionInfos, componentScope: angular.IScope) => {
				const newViewportValues = this.viewportModule.updateWholeViewport(propValue, internalState, wholeViewport, conversionInfos, componentScope, propertyContext);
				
				propValue.splice(0, propValue.length); // we want to keep the same main value reference; so clear all old items and add the new ones
				for (let tz = 0; tz < newViewportValues.length; tz++) propValue.push(newViewportValues[tz]);
				
				if (propValue && internalState && internalState.changeListeners.length > 0) {
					const notificationParamForListeners = {};
					notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED] = { oldValue: propValue, newValue: propValue }; // should we not set oldValue here? old one has changed into new one so basically we do not have old content anymore...
					
					if (this.log.debugEnabled && this.log.debugLevel === this.log.SPAM) this.log.debug("svy foundset linked * firing change listener: full viewport changed...");
					// use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
					internalState.fireChanges(notificationParamForListeners);
				}
			}
		}

		private addBackWatches(value, componentScope: angular.IScope) {
			if (angular.isDefined(value) && value !== null) {
				const iS = value[this.sabloConverters.INTERNAL_IMPL];
				
				this.viewportModule.addDataWatchesToRows(value, iS, componentScope, true, iS[FoundsetLinkedType.PUSH_TO_SERVER]);
				
				if (componentScope && iS.singleValueState) {
					// watch foundSet viewport size; when it changes generate a new viewport client side as this is a repeated single value; it is not record linked

					iS.singleValueState.viewportSizeUnwatch = componentScope.$watch(() => {
						return this.sabloUtils.getInDepthProperty(iS[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY](), "viewPort", "size")
					}, (newViewportSize) => {
								if (newViewportSize === iS.singleValueState.initialVPSize) return;
								iS.singleValueState.initialVPSize = -1;
								if (!angular.isDefined(newViewportSize)) newViewportSize = 0;
								
								componentScope.$evalAsync(function() {
									this.viewportModule.removeDataWatchesFromRows(value.length, iS);
									const wholeViewport = iS.singleValueState.generateWholeViewportFromOneValue(iS, newViewportSize);
									iS.singleValueState.updateWholeViewport(value, iS, wholeViewport, iS.singleValueState.conversionInfos, componentScope);
									this.viewportModule.addDataWatchesToRows(value, iS, componentScope, true, iS[FoundsetLinkedType.PUSH_TO_SERVER]);
								})
							});
				}
			}
		}
		
		private removeAllWatches(value) {
			if (value != null && angular.isDefined(value)) {
				const iS = value[this.sabloConverters.INTERNAL_IMPL];
				this.viewportModule.removeDataWatchesFromRows(value.length, iS);
				if (iS.singleValueState && iS.singleValueState.viewportSizeUnwatch) {
					iS.singleValueState.viewportSizeUnwatch();
					iS.singleValueState.viewportSizeUnwatch = undefined;
				}
			}
		}

		private handleSingleValue(singleValue, iS, conversionInfoFromServer) {
			// this gets called for values that are not actually record linked, and we 'fake' a viewport containing the same value on each row in the array
			iS[this.foundsetLinkedTypeConstants.RECORD_LINKED] = false;
			
			// *** BEGIN we need the following in addBackWatches that is also called by updateAngularScope, that is why they are stored in internalState (iS)
			iS.singleValueState.generateWholeViewportFromOneValue = function(internalState, vpSize) {
				if (angular.isUndefined(vpSize)) vpSize = 0;
				const wholeViewport = [];
				if (conversionInfoFromServer) {
					// we got from server the conversion type for this single value; as we generate a viewport of that value we must give the ViewportService code
					// optimized conversion info as one would receive from server for a full viewport
					const optimizedServerSentConversionsEquivalent = {};
					optimizedServerSentConversionsEquivalent[ViewportService.MAIN_TYPE] = conversionInfoFromServer;
					internalState.singleValueState.conversionInfos = optimizedServerSentConversionsEquivalent; 
				} else internalState.singleValueState.conversionInfos = undefined; 
				
				for (let index = vpSize - 1; index >= 0; index--) {
					wholeViewport.push(singleValue);
				}
				return wholeViewport;
			}
			iS.singleValueState.initialVPSize = this.sabloUtils.getInDepthProperty(iS[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY](), "viewPort", "size");
			// *** END
			
			return iS.singleValueState.generateWholeViewportFromOneValue(iS, iS.singleValueState.initialVPSize);
		}
	
		public fromServerToClient(serverJSONValue: any, currentClientValue: FoundsetLinkedValue, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): FoundsetLinkedValue {
			const newValue = (currentClientValue ? currentClientValue : new FoundsetLinkedValue(this.sabloConverters, componentScope, this.foundsetLinkedTypeConstants, this.webSocket));

			// remove watches to avoid an unwanted detection of received changes
			this.removeAllWatches(currentClientValue);

			if (serverJSONValue != null && angular.isDefined(serverJSONValue)) {
				let didSomething = false;
				const internalState = newValue[this.sabloConverters.INTERNAL_IMPL];

				if (angular.isDefined(serverJSONValue[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
					// the foundset that this property is linked to; keep that info in internal state; viewport.js needs it
					const forFoundsetPropertyName = serverJSONValue[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
					internalState[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] = function() {
						return propertyContext(forFoundsetPropertyName);
					};
					didSomething = true;
				}

				if (typeof serverJSONValue[FoundsetLinkedType.PUSH_TO_SERVER] !== 'undefined') {
					internalState[FoundsetLinkedType.PUSH_TO_SERVER] = serverJSONValue[FoundsetLinkedType.PUSH_TO_SERVER];
				}

				if (angular.isDefined(serverJSONValue[FoundsetLinkedType.VIEWPORT_VALUE_UPDATE])) {
					internalState.singleValueState = undefined;
					internalState[this.foundsetLinkedTypeConstants.RECORD_LINKED] = true;
					
					this.viewportModule.updateViewportGranularly(newValue, internalState, serverJSONValue[FoundsetLinkedType.VIEWPORT_VALUE_UPDATE],
							componentScope, propertyContext, true);
					if (this.log.debugEnabled && this.log.debugLevel === this.log.SPAM) this.log.debug("svy foundset linked * firing change listener: granular updates...");
					internalState.fireChanges(serverJSONValue[FoundsetLinkedType.VIEWPORT_VALUE_UPDATE]);
				} else {
					// the rest will always be treated as a full viewport update (single values are actually going to generate a full viewport of 'the one' new value)
					let conversionInfos;
					const updateWholeViewportFunc = this.getUpdateWholeViewportFunc(propertyContext);
					
					let wholeViewport;
					if (angular.isDefined(serverJSONValue[FoundsetLinkedType.SINGLE_VALUE]) || angular.isDefined(serverJSONValue[FoundsetLinkedType.SINGLE_VALUE_UPDATE])) {
						// just update single value from server and make copies of it to duplicate
						const conversionInfo = serverJSONValue[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY];
						const singleValue = angular.isDefined(serverJSONValue[FoundsetLinkedType.SINGLE_VALUE]) ?
								serverJSONValue[FoundsetLinkedType.SINGLE_VALUE] : serverJSONValue[FoundsetLinkedType.SINGLE_VALUE_UPDATE];
						internalState.singleValueState = {};
						internalState.singleValueState.updateWholeViewport = updateWholeViewportFunc;
						wholeViewport = this.handleSingleValue(singleValue, internalState, conversionInfo);
						conversionInfos = internalState.singleValueState.conversionInfos;
						// addBackWatches below (end of function) will add a watch for foundset prop. size to regenerate the viewport when that changes - fill it up with single values
					} else if (angular.isDefined(serverJSONValue[FoundsetLinkedType.VIEWPORT_VALUE])) {
						internalState.singleValueState = undefined;
						internalState[this.foundsetLinkedTypeConstants.RECORD_LINKED] = true;
						
						wholeViewport = serverJSONValue[FoundsetLinkedType.VIEWPORT_VALUE];
						conversionInfos = serverJSONValue[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY];
					}
					
					if (angular.isDefined(wholeViewport)) updateWholeViewportFunc(newValue, internalState, wholeViewport, conversionInfos, componentScope);
					else if (!didSomething) this.log.error("Can't interpret foundset linked prop. server update correctly: " + JSON.stringify(serverJSONValue, undefined, 2));
				}
			}
			
			if (serverJSONValue[this.foundsetLinkedTypeConstants.ID_FOR_FOUNDSET] === null) {
				if (angular.isDefined(newValue[this.foundsetLinkedTypeConstants.ID_FOR_FOUNDSET])) delete newValue[this.foundsetLinkedTypeConstants.ID_FOR_FOUNDSET];
			} else if (angular.isDefined(serverJSONValue[this.foundsetLinkedTypeConstants.ID_FOR_FOUNDSET])) {
				// make it non-iterable as the newValue is an array an ppl. might iterate over it - they wont expect this in the iterations
				if (Object.defineProperty) {
					Object.defineProperty(newValue, this.foundsetLinkedTypeConstants.ID_FOR_FOUNDSET, {
						configurable: true,
						enumerable: false,
						writable: true,
						value: serverJSONValue[this.foundsetLinkedTypeConstants.ID_FOR_FOUNDSET]
					});
				} else newValue[this.foundsetLinkedTypeConstants.ID_FOR_FOUNDSET] = serverJSONValue[this.foundsetLinkedTypeConstants.ID_FOR_FOUNDSET];
			}
			
			// restore/add model watch
			this.addBackWatches(newValue, componentScope);
			return newValue;
		}

		public updateAngularScope(clientValue: FoundsetLinkedValue, componentScope: angular.IScope) {
			this.removeAllWatches(clientValue);
			if (componentScope) this.addBackWatches(clientValue, componentScope);

			if (clientValue) {
				const internalState = clientValue[this.sabloConverters.INTERNAL_IMPL];
				if (internalState) {
					this.viewportModule.updateAngularScope(clientValue, internalState, componentScope, true);
				}
			}
		}

		public fromClientToServer(newClientData: any, oldClientData: FoundsetLinkedValue) {
			if (newClientData) {
				const internalState = newClientData[this.sabloConverters.INTERNAL_IMPL];
				if (internalState.isChanged()) {
					if (!internalState[this.foundsetLinkedTypeConstants.RECORD_LINKED]) {
						// we don't need to send rowId to server in this case; we just need value
						for (const index in internalState.requests) {
							internalState.requests[index][FoundsetLinkedType.PROPERTY_CHANGE] = internalState.requests[index].viewportDataChanged.value;
							delete internalState.requests[index].viewportDataChanged;
						}
					}
					const tmp = internalState.requests;
					internalState.requests = [];
					return tmp;
				}
			}
			return [];
		}
		
	}
	
	class FoundsetLinkedValue extends Array<any> {
		
		/** Initializes internal state of a new value */
		constructor (private readonly sabloConverters: sablo.ISabloConverters,
				componentScope: angular.IScope, foundsetLinkedTypeConstants, webSocket: sablo.IWebSocket) {
			super();
			
			sabloConverters.prepareInternalState(this);
			const internalState = this[sabloConverters.INTERNAL_IMPL];

			// implement what $sabloConverters need to make this work
			internalState.setChangeNotifier = function(changeNotifier) {
				internalState.changeNotifier = changeNotifier; 
			}
			internalState.isChanged = function() {
				return internalState.requests && (internalState.requests.length > 0);;
			}

			// private impl
			internalState[foundsetLinkedTypeConstants.RECORD_LINKED] = false;
			internalState.singleValueState = undefined;
			internalState.conversionInfo = [];
			internalState.requests = []; // see viewport.js for how this will get populated
			
			internalState.fireChanges = function(values) {
				for (let i = 0; i < internalState.changeListeners.length; i++) {
					webSocket.setIMHDTScopeHintInternal(componentScope);
					internalState.changeListeners[i](values);
					webSocket.setIMHDTScopeHintInternal(undefined);
				}
			}
		}
		
		/**
		 * Adds a change listener that will get triggered when server sends changes for this foundset linked property.
		 * 
		 * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
		 * @param listener the listener to register.
		 */
		public addChangeListener(listener) {
			this[this.sabloConverters.INTERNAL_IMPL].changeListeners.push(listener);
			return () => { return this.removeChangeListener(listener); };
		}
		
		public removeChangeListener(listener) {
			const internalState = this[this.sabloConverters.INTERNAL_IMPL];

			const index = internalState.changeListeners.indexOf(listener);
			if (index > -1) {
				internalState.changeListeners.splice(index, 1);
			}
		}
		
	}
	
}
