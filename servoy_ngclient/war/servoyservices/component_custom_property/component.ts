/// <reference path="../../../typings/angularjs/angular.d.ts" />
/// <reference path="../../../typings/servoy/component.d.ts" />
/// <reference path="../../../typings/servoy/foundset.d.ts" />
/// <reference path="../../../typings/sablo/sablo.d.ts" />
/// <reference path="../foundset_viewport_module/viewport.ts" />

angular.module('component_custom_property', ['webSocketModule', 'servoyApp', 'foundset_custom_property', 'foundset_viewport_module', '$sabloService'])
//Component type ------------------------------------------
.value("$componentTypeConstants", {
	CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
	CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
})
.run(function ($sabloConverters: sablo.ISabloConverters, $sabloUtils: sablo.ISabloUtils, $viewportModule: ngclient.propertyTypes.ViewportService,
		$servoyInternal, $log: sablo.ILogService, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants, $propertyWatchesRegistry,
		$sabloService, $webSocket: sablo.IWebSocket, $uiBlocker, $q: angular.IQService, $typesRegistry: sablo.ITypesRegistry) {
	
	$typesRegistry.registerGlobalType('component', new ngclient.propertyTypes.ComponentType($sabloConverters, $sabloUtils, $viewportModule,
			$servoyInternal, $log, $foundsetTypeConstants, $propertyWatchesRegistry,
			$sabloService, $webSocket, $uiBlocker, $q, $typesRegistry));
});
	
namespace ngclient.propertyTypes {

	export class ComponentType implements sablo.IType<ComponentValue> {
		
		static readonly PROPERTY_UPDATES_KEY = "propertyUpdates";

		static readonly MODEL_KEY = "model";
		static readonly MODEL_VIEWPORT_KEY = "model_vp";
		static readonly MODEL_VIEWPORT_CHANGES_KEY = "model_vp_ch";
		static readonly MODEL_VIEWPORT = "modelViewport";

		static readonly PROPERTY_NAME_KEY = "pn";
		static readonly VALUE_KEY = "v";
		
		static readonly NO_OP = "n";
		
		constructor(private readonly sabloConverters: sablo.ISabloConverters, private readonly sabloUtils: sablo.ISabloUtils,
				private readonly viewportModule: ngclient.propertyTypes.ViewportService, private readonly servoyInternal,
				private readonly log: sablo.ILogService, private readonly foundsetTypeConstants: foundsetType.FoundsetTypeConstants,
				private readonly propertyWatchesRegistry, private readonly sabloService, private readonly webSocket: sablo.IWebSocket,
				private readonly uiBlocker, private readonly q: angular.IQService, private readonly typesRegistry: sablo.ITypesRegistry) {}

		private getChildPropertyChanges(componentState: ComponentValue, oldPropertyValue: any, newPropertyValue: any, propertyName: string) {
			const internalState = componentState[this.sabloConverters.INTERNAL_IMPL];
			
			// just dummy stuff - currently the parent controls layout, but getComponentChanges needs such args...
			const containerSize = {width: 0, height: 0};
	
			return this.servoyInternal.getComponentChanges(newPropertyValue, oldPropertyValue, componentState.componentDirectiveName,
					internalState.beanLayout, containerSize, propertyName, componentState.model);
		}
	
		private getBeanPropertyChangeNotifierGenerator(componentPropertyValue: ComponentValue): (propertyName: string) => ((oldValue, newValue, dumb) => void) {
			return /*beanPropertyChangeNotifierGenerator*/ (propertyName: string): ((oldValue, newValue, dumb) => void) => {
				if (!componentPropertyValue) return undefined;
	
				const internalState = componentPropertyValue[this.sabloConverters.INTERNAL_IMPL];
				return /*beanPropertyChangeNotifier*/ (oldValue, newValue, dumb) => { // oldValue, newValue and dumb are only set when called from bean model in-depth/shallow watch; not set for smart properties
					if (dumb !== true) {
						// so smart property - no watch involved (it notifies itself as changed)
						oldValue = newValue = componentPropertyValue[ComponentType.MODEL_KEY][propertyName];
					} 
					internalState.requests.push({ propertyChanges : this.getChildPropertyChanges(componentPropertyValue, oldValue, newValue, propertyName) });
					if (internalState.changeNotifier) internalState.changeNotifier();
				};
			};
		}
	
		private watchModel(componentTypeName, beanModel, childChangedNotifierGenerator, componentScope) {
			// $propertyWatchesRegistry knows exactly which properties are to be watched based on component type
			return this.propertyWatchesRegistry.watchDumbPropertiesForComponent(componentScope, componentTypeName, beanModel, (newValue, oldValue, property) => {
				childChangedNotifierGenerator(property)(oldValue, newValue, true);
			});
		}
	
		private removeAllWatches(value) {
			if (value != null && angular.isDefined(value)) {
				const iS = value[this.sabloConverters.INTERNAL_IMPL];
				if (iS.modelUnwatch) {
					for (const unW in iS.modelUnwatch)
						iS.modelUnwatch[unW]();
					iS.modelUnwatch = null;
				}
				if (value[ComponentType.MODEL_VIEWPORT]) this.viewportModule.removeDataWatchesFromRows(value[ComponentType.MODEL_VIEWPORT].length, iS);
			}
		}
	
		private addBackWatches(value, componentScope, childChangedNotifierGenerator) {
			if (angular.isDefined(value) && value !== null) {
				const iS = value[this.sabloConverters.INTERNAL_IMPL];
	
				let propertiesToWatch = this.propertyWatchesRegistry.getPropertiesToAutoWatchForComponent(value.componentDirectiveName);
				if (typeof propertiesToWatch === 'undefined') propertiesToWatch = {}; // that will not add watches for any column; don't send undefined here as that would mean default (add to all)
	
				if (value[ComponentType.MODEL_VIEWPORT]) this.viewportModule.addDataWatchesToRows(value[ComponentType.MODEL_VIEWPORT], iS, componentScope, false, propertiesToWatch);
				if (componentScope) iS.modelUnwatch = this.watchModel(value.componentDirectiveName, value.model, childChangedNotifierGenerator, componentScope);
			}
		}
	
		public fromServerToClient(serverJSONValue: any, currentClientValue: ComponentValue, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): ComponentValue {
			let newValue: ComponentValue = currentClientValue;

			// see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
			const hasListeners = (currentClientValue && currentClientValue[this.sabloConverters.INTERNAL_IMPL].viewportChangeListeners.length > 0);
			const notificationParamForListeners = hasListeners ? { } : undefined;

			// remove watches to avoid an unwanted detection of received changes
			this.removeAllWatches(currentClientValue);

			let childChangedNotifierGenerator; 
			if (serverJSONValue && serverJSONValue[ComponentType.PROPERTY_UPDATES_KEY]) {
				// granular updates received
				childChangedNotifierGenerator = this.getBeanPropertyChangeNotifierGenerator(currentClientValue); 

				const internalState = currentClientValue[this.sabloConverters.INTERNAL_IMPL];
				const beanUpdate = serverJSONValue[ComponentType.PROPERTY_UPDATES_KEY];

				const modelBeanUpdate = beanUpdate[ComponentType.MODEL_KEY];
				const wholeViewportUpdate = beanUpdate[ComponentType.MODEL_VIEWPORT_KEY];
				const viewportUpdate = beanUpdate[ComponentType.MODEL_VIEWPORT_CHANGES_KEY];
				let done = false;

				if (modelBeanUpdate) {
					const beanModel = currentClientValue[ComponentType.MODEL_KEY];

					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					const beanLayout = internalState.beanLayout;
					const containerSize = {width: 0, height: 0};

					this.servoyInternal.applyBeanData(beanModel, beanLayout, modelBeanUpdate, containerSize, childChangedNotifierGenerator,
							newValue.componentDirectiveName, componentScope);
					done = true;
				}

				// if component is linked to a foundset, then record - dependent property values are sent over as as viewport representing values for the foundset property's viewport
				if (wholeViewportUpdate) {
					const oldRows = currentClientValue[ComponentType.MODEL_VIEWPORT];
					if (!angular.isDefined(oldRows)) currentClientValue[ComponentType.MODEL_VIEWPORT] = [];

					this.viewportModule.updateWholeViewport(currentClientValue, ComponentType.MODEL_VIEWPORT,
							internalState, wholeViewportUpdate, beanUpdate[this.sabloConverters.TYPES_KEY] && beanUpdate[this.sabloConverters.TYPES_KEY][ComponentType.MODEL_VIEWPORT_KEY] ?
									beanUpdate[this.sabloConverters.TYPES_KEY][ComponentType.MODEL_VIEWPORT_KEY] : undefined, componentScope, () => {
										return currentClientValue[ComponentType.MODEL_KEY];
									});
					if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED] = { oldValue: oldRows, newValue: currentClientValue[MODEL_VIEWPORT] };
					done = true;
				} else if (viewportUpdate) {
					const oldSize = currentClientValue[ComponentType.MODEL_VIEWPORT].length;
					this.viewportModule.updateViewportGranularly(currentClientValue[ComponentType.MODEL_VIEWPORT], internalState, viewportUpdate,
							beanUpdate[this.sabloConverters.TYPES_KEY] && beanUpdate[this.sabloConverters.TYPES_KEY][ComponentType.MODEL_VIEWPORT_CHANGES_KEY] ?
									beanUpdate[this.sabloConverters.TYPES_KEY][ComponentType.MODEL_VIEWPORT_CHANGES_KEY] : undefined, componentScope, () => {
										return currentClientValue[ComponentType.MODEL_KEY];
									}, false);
					if (hasListeners) {
						notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED] = { updates : viewportUpdate }; // viewPortUpdate was already prepared for listeners by $viewportModule.updateViewportGranularly
						notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED][this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE] = oldSize; // no longer needed/deprecated starting with Servoy 8.4 where granular update indexes are guaranteed to match final state of viewport data so less processing is needed
					}

					done = true;
				}

				if (!done) {
					this.log.error("Can't interpret component server update correctly: " + JSON.stringify(serverJSONValue, undefined, 2));
				}
			} else if (!angular.isDefined(serverJSONValue) || !serverJSONValue[ComponentType.NO_OP]) {
				// full contents received
				if (serverJSONValue) {
					newValue = new ComponentValue(this.sabloConverters);
					
					for (const propName in Object.getOwnPropertyNames(serverJSONValue)) {
						newValue[propName] = serverJSONValue[propName];
					}

					this.sabloConverters.prepareInternalState(this);
					childChangedNotifierGenerator = this.getBeanPropertyChangeNotifierGenerator(newValue);
	
					const internalState = newValue[this.sabloConverters.INTERNAL_IMPL];
	
					if (angular.isDefined(newValue[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
						// if it's linked to a foundset, keep that info in internal state; viewport.js needs it
						const forFoundsetPropertyName = newValue[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
						internalState[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] = () => {
							return propertyContext(forFoundsetPropertyName);
						};
						delete newValue[this.foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
					}
					const executeHandler = (type, args, row, name, model) => {		
						if (this.uiBlocker.shouldBlockDuplicateEvents(name, model, type, row))
						{
							// reject execution
							console.log("rejecting execution of: "+type +" on "+name +" row "+row);
							return this.q.resolve(null);
						}
						
						const promiseAndCmsid = this.sabloService.createDeferedEvent();
						const newargs = this.sabloUtils.getEventArgs(args,type);
						internalState.requests.push({ handlerExec: {
							eventType: type,
							args: newargs,
							rowId: row,
							defid: promiseAndCmsid.defid
						}});
						if (internalState.changeNotifier) internalState.changeNotifier();
						promiseAndCmsid.promise.finally(() => { this.uiBlocker.eventExecuted(name, model, type, row); });
						return promiseAndCmsid.promise;
					};
	
					// implement what $sabloConverters need to make this work
					internalState.setChangeNotifier = (changeNotifier) => {
						internalState.changeNotifier = changeNotifier; 
					}
					internalState.isChanged = () => { return internalState.requests && (internalState.requests.length > 0); }
	
					// private impl
					internalState.requests = [];
					internalState.beanLayout = null; // not really useful right now; just to be able to reuse existing form code 
	
					// even if it's a completely new value, keep listeners from old one if there is an old value
					internalState.viewportChangeListeners = (currentClientValue && currentClientValue[this.sabloConverters.INTERNAL_IMPL] ? currentClientValue[this.sabloConverters.INTERNAL_IMPL].viewportChangeListeners : []);
	
					internalState.fireChanges = function(viewportChanges) {
						for(const i = 0; i < internalState.viewportChangeListeners.length; i++) {
							this.webSocket.setIMHDTScopeHintInternal(componentScope);
							internalState.viewportChangeListeners[i](viewportChanges);
							this.webSocket.setIMHDTScopeHintInternal(undefined);
						}
					}
	
					internalState.modelUnwatch = null;
	
					// calling applyBeanData initially to make sure any needed conversions are done on model's properties
					const beanData = newValue[ComponentType.MODEL_KEY];
					const beanModel = new sablo_app.Model();
					newValue[ComponentType.MODEL_KEY] = beanModel;
	
					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					internalState.beanLayout = {};
					const containerSize = {width: 0, height: 0};
	
					const currentConversionInfo = beanData[this.sabloConverters.TYPES_KEY] ?
							this.sabloUtils.getOrCreateInDepthProperty(internalState, this.sabloConverters.TYPES_KEY) :
								this.sabloUtils.getInDepthProperty(internalState, this.sabloConverters.TYPES_KEY);
	
					this.servoyInternal.applyBeanData(beanModel, internalState.beanLayout, beanData, containerSize, childChangedNotifierGenerator,
							currentConversionInfo, beanData[this.sabloConverters.TYPES_KEY], componentScope);
	
					// component property is now be able to send itself entirely at runtime; we need to handle viewport conversions here as well
					const wholeViewport = newValue[ComponentType.MODEL_VIEWPORT_KEY];
					delete newValue[ComponentType.MODEL_VIEWPORT_KEY];
					newValue[ComponentType.MODEL_VIEWPORT] = [];
	
					if (wholeViewport) {
						this.viewportModule.updateWholeViewport(newValue, ComponentType.MODEL_VIEWPORT,
								internalState, wholeViewport, newValue[this.sabloConverters.TYPES_KEY] ?
										newValue[this.sabloConverters.TYPES_KEY][ComponentType.MODEL_VIEWPORT_KEY] : undefined, componentScope, () => {
											return newValue[ComponentType.MODEL_KEY];
										});
					}
					if (angular.isDefined(newValue[this.sabloConverters.TYPES_KEY])) delete newValue[this.sabloConverters.TYPES_KEY];
	
					if (!newValue.api) newValue.api = {};
					if (newValue.handlers)
					{
						for (const key in newValue.handlers) 
						{
							const handler = newValue.handlers[key];
							((key) => {
								const eventHandler = (args,rowId) => {
									return executeHandler(key, args, rowId, newValue.name, newValue.model);
								}
								const wrapper = () => {
									return eventHandler(arguments, null);
								}
								wrapper.selectRecordHandler = (rowId) => {
									return () => { 
										return eventHandler(arguments,rowId instanceof Function ? rowId() : rowId) 
									}
								};
								newValue.handlers[key] = wrapper;
							})(key);
						}
					}
	
					// here we don't specify any of the following as all those can be forwarded by the parent component from it's own servoyApi:
					// formWillShow, hideForm, getFormUrl, isInDesigner, getFormComponentElements, 
					newValue.servoyApi = {
						/** rowId is only needed if the component is linked to a foundset */
						startEdit: (property, rowId) => {
							const req = { svyStartEdit: {} };
	
							if (rowId) req.svyStartEdit[this.foundsetTypeConstants.ROW_ID_COL_KEY] = rowId;
							req.svyStartEdit[ComponentType.PROPERTY_NAME_KEY] = property;
	
							internalState.requests.push(req);
							if (internalState.changeNotifier) internalState.changeNotifier();
						},
	
						apply: (property, modelOfComponent, rowId) => {
							/** rowId is only needed if the component is linked to a foundset */
							const conversionInfo = internalState[this.sabloConverters.TYPES_KEY];
							if (!modelOfComponent) modelOfComponent = newValue[ComponentType.MODEL_KEY]; // if it's not linked to foundset componentModel will be undefined
							const propertyValue = modelOfComponent[property];
	
							if (conversionInfo && conversionInfo[property]) {
								propertyValue = this.sabloConverters.convertFromClientToServer(propertyValue, conversionInfo[property], undefined);
							} else {
								propertyValue = this.sabloUtils.convertClientObject(propertyValue);
							}
	
							const req = { svyApply: {} };
	
							if (rowId) req.svyApply[this.foundsetTypeConstants.ROW_ID_COL_KEY] = rowId;
							req.svyApply[ComponentType.PROPERTY_NAME_KEY] = property;
							req.svyApply[ComponentType.VALUE_KEY] = propertyValue;
	
							internalState.requests.push(req);
							if (internalState.changeNotifier) internalState.changeNotifier();
						}
					}
				} else newValue = undefined;
			}

			// restore/add model watch
			this.addBackWatches(newValue, componentScope, childChangedNotifierGenerator);
			
			if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
				if (this.log.debugEnabled && this.log.debugLevel === this.log.SPAM) this.log.debug("svy component * firing founset listener notifications: " + JSON.stringify(Object.keys(notificationParamForListeners)));
				// use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
				currentClientValue[this.sabloConverters.INTERNAL_IMPL].fireChanges(notificationParamForListeners);
			}

			return newValue;
		}

		public updateAngularScope(clientValue: ComponentValue, componentScope: angular.IScope): void {
			this.removeAllWatches(clientValue);
			if (componentScope) this.addBackWatches(clientValue, componentScope, this.getBeanPropertyChangeNotifierGenerator(clientValue));

			if (clientValue) {
				const internalState = clientValue[this.sabloConverters.INTERNAL_IMPL];
				if (internalState) {
					const conversionInfo = internalState[this.sabloConverters.TYPES_KEY];
					if (conversionInfo) {
						const beanModel = clientValue[ComponentType.MODEL_KEY];
						for (const key in beanModel) {
							if (conversionInfo[key]) this.sabloConverters.updateAngularScope(beanModel[key], conversionInfo[key], componentScope);
						}
					}

					this.viewportModule.updateAngularScope(clientValue[ComponentType.MODEL_VIEWPORT], internalState, componentScope, false);
				}
			}
		}

		public fromClientToServer(newClientData: any, oldClientData: ComponentValue): any {
			if (newClientData) {
				const internalState = newClientData[this.sabloConverters.INTERNAL_IMPL];
				if (internalState.isChanged()) {
					const tmp = internalState.requests;
					internalState.requests = [];
					return tmp;
				}
			}
			return [];
		}
		
	}
	
	class ComponentValue implements componentType.ComponentPropertyValue {
		
		name: string;
		componentDirectiveName: string;
		model: any;
		handlers: any;
		api: any;
		foundsetConfig?: {
			recordBasedProperties?: Array<string>;
			apiCallTypes?: Array<any>
		};
		servoyApi: {
			startEdit: ( property: string, rowId: object ) => void;
			apply: ( property: string, modelOfComponent: object, rowId: object ) => void;
		};
	
		constructor(private readonly sabloConverters: sablo.ISabloConverters) {}
		
		/**
		 * Adds a change listener that will get triggered when server sends granular or full modelViewport changes for this component.
		 * 
		 * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this same foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
		 * @param viewportChangeListener the listener to register.
		 * 
		 * @return a listener unregister function.
		 */
		public addViewportChangeListener(viewportChangeListener : componentType.ViewportChangeListener) : () => void {
			internalState.viewportChangeListeners.push(viewportChangeListener);
			return () => { this.removeViewportChangeListener(viewportChangeListener); };
		}
		
		public removeViewportChangeListener(viewportChangeListener : componentType.ViewportChangeListener) : void {
			const index = internalState.viewportChangeListeners.indexOf(viewportChangeListener);
			if (index > -1) {
				internalState.viewportChangeListeners.splice(index, 1);
			}
		}
		
	}
	
}
