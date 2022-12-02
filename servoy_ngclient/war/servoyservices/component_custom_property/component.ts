/// <reference path="../../../typings/angularjs/angular.d.ts" />
/// <reference path="../../../typings/servoy/component.d.ts" />
/// <reference path="../../../typings/servoy/foundset.d.ts" />
/// <reference path="../../../typings/sablo/sablo.d.ts" />
/// <reference path="../foundset_viewport_module/viewport.ts" />

angular.module('component_custom_property', ['webSocketModule', 'servoyApp', 'foundset_custom_property', 'foundset_viewport_module', '$sabloService'])
//Component type ------------------------------------------
.value("$componentTypeConstants", {
	CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
	CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1,
	CHILD_COMPONENT_TYPE_NAME: 'component'
})
.run(function ($sabloConverters: sablo.ISabloConverters, $sabloUtils: sablo.ISabloUtils, $viewportModule: ngclient.propertyTypes.ViewportService,
		$servoyInternal, $log: sablo.ILogService, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants, $propertyWatchUtils: sablo.IPropertyWatchUtils,
		$pushToServerUtils: sablo.IPushToServerUtils, $sabloService, $webSocket: sablo.IWebSocket, $uiBlocker, $q: angular.IQService, $typesRegistry: sablo.ITypesRegistry,
		$sabloApplication: sablo.ISabloApplication, $componentTypeConstants) {
	
	$typesRegistry.registerGlobalType($componentTypeConstants.CHILD_COMPONENT_TYPE_NAME, new ngclient.propertyTypes.ComponentType($sabloConverters, $sabloUtils, $viewportModule,
			$servoyInternal, $log, $foundsetTypeConstants, $propertyWatchUtils, $pushToServerUtils,
			$sabloService, $webSocket, $uiBlocker, $q, $typesRegistry, $sabloApplication), false);
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
				private readonly propertyWatchUtils: sablo.IPropertyWatchUtils, private readonly pushToServerUtils: sablo.IPushToServerUtils,
				private readonly sabloService, private readonly webSocket: sablo.IWebSocket,
				private readonly uiBlocker, private readonly q: angular.IQService, private readonly typesRegistry: sablo.ITypesRegistry,
				private readonly sabloApplication: sablo.ISabloApplication) {}

		private watchModel(componentTypeName, beanModel, modelPropChangedNotifierGenerator, componentScope) {
			// $propertyWatchUtils knows exactly which properties are to be watched based on component type
			return this.propertyWatchUtils.watchDumbPropertiesForComponent(componentScope, componentTypeName, beanModel, (newValue, oldValue, property) => {
				modelPropChangedNotifierGenerator(property)(oldValue, newValue, true);
			});
		}
	
		private removeAllWatches(value) {
			if (value != null && angular.isDefined(value)) {
				const iS: ComponentTypeInternalState = value[this.sabloConverters.INTERNAL_IMPL];
				if (iS.modelUnwatch) {
					for (const unW in iS.modelUnwatch)
						iS.modelUnwatch[unW]();
					iS.modelUnwatch = null;
				}
				if (value[ComponentType.MODEL_VIEWPORT]) this.viewportModule.removeDataWatchesFromRows(iS, value[ComponentType.MODEL_VIEWPORT], false);
			}
		}
	
		private addBackWatches(value, componentScope, modelPropChangedNotifierGenerator) {
			if (angular.isDefined(value) && value !== null) {
				const iS: ComponentTypeInternalState = value[this.sabloConverters.INTERNAL_IMPL];
	
				if (value[ComponentType.MODEL_VIEWPORT]) this.viewportModule.addDataWatchesToRows(value[ComponentType.MODEL_VIEWPORT], iS, 
						componentScope, iS.propertyContextCreator, false );
				if (componentScope) iS.modelUnwatch = this.watchModel(value.componentDirectiveName, value.model, modelPropChangedNotifierGenerator, componentScope);
			}
		}
	
		public fromServerToClient(serverJSONValue: any, currentClientValue: ComponentValue, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): ComponentValue {
			let newValue: ComponentValue = currentClientValue;

			// see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
			const hasListeners = (currentClientValue && (currentClientValue[this.sabloConverters.INTERNAL_IMPL] as ComponentTypeInternalState).changeListeners.length > 0);
			const notificationParamForListeners = hasListeners ? { } : undefined;

			// remove watches to avoid an unwanted detection of received changes
			this.removeAllWatches(currentClientValue);

			let modelPropChangedNotifierGenerator:sablo.PropertyChangeNotifierGeneratorFunction;
			if (serverJSONValue && serverJSONValue[ComponentType.PROPERTY_UPDATES_KEY]) {
				// granular updates received

				const internalState: ComponentTypeInternalState = currentClientValue[this.sabloConverters.INTERNAL_IMPL];
				const beanUpdate = serverJSONValue[ComponentType.PROPERTY_UPDATES_KEY];

				modelPropChangedNotifierGenerator = internalState.getModelPropertyChangeNotifierGenerator();
				 
				const modelBeanUpdate = beanUpdate[ComponentType.MODEL_KEY];
				const wholeViewportUpdate = beanUpdate[ComponentType.MODEL_VIEWPORT_KEY];
				const viewportUpdate = beanUpdate[ComponentType.MODEL_VIEWPORT_CHANGES_KEY];
				let done = false;
				
				if (modelBeanUpdate) {
					const beanModel = currentClientValue[ComponentType.MODEL_KEY];

					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					const beanLayout = internalState.beanLayout;
					const containerSize = {width: 0, height: 0};

					this.servoyInternal.applyBeanData(beanModel, beanLayout, modelBeanUpdate, containerSize, modelPropChangedNotifierGenerator,
							newValue.componentDirectiveName, internalState.dynamicTypesForNonViewportProperties, componentScope);
					done = true;
				}

				// if component is linked to a foundset, then record - dependent property values are sent over as as viewport representing values for the foundset property's viewport
				if (wholeViewportUpdate) {
					const oldRows = currentClientValue[ComponentType.MODEL_VIEWPORT];

					currentClientValue[ComponentType.MODEL_VIEWPORT] = this.viewportModule.updateWholeViewport(oldRows,
							internalState, wholeViewportUpdate, beanUpdate[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY],
							this.typesRegistry.getComponentSpecification(newValue.componentDirectiveName), componentScope,
							internalState.propertyContextCreator, false);
					if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED] = { oldValue: oldRows, newValue: currentClientValue[ComponentType.MODEL_VIEWPORT] };
					done = true;
				} else if (viewportUpdate) {
					const oldSize = currentClientValue[ComponentType.MODEL_VIEWPORT].length;
					this.viewportModule.updateViewportGranularly(currentClientValue[ComponentType.MODEL_VIEWPORT], internalState, viewportUpdate,
							this.typesRegistry.getComponentSpecification(newValue.componentDirectiveName), componentScope,
							internalState.propertyContextCreator, false);
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
					newValue = new ComponentValue(serverJSONValue, currentClientValue, componentScope, propertyContext,
							this.sabloConverters, this.sabloUtils, this.sabloService, this.uiBlocker, this.foundsetTypeConstants,
							this.viewportModule, this.q, this.webSocket, this.servoyInternal, this.typesRegistry,
							this.sabloApplication, this.pushToServerUtils, this.log);

                    const internalState: ComponentTypeInternalState = newValue[this.sabloConverters.INTERNAL_IMPL];
                    modelPropChangedNotifierGenerator = internalState.getModelPropertyChangeNotifierGenerator(); 
				} else newValue = undefined;
			}

			// restore/add model watch
			this.addBackWatches(newValue, componentScope, modelPropChangedNotifierGenerator);
			
			if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
				if (this.log.debugEnabled && this.log.debugLevel === this.log.SPAM) this.log.debug("svy component * firing founset listener notifications: " + JSON.stringify(Object.keys(notificationParamForListeners)));
				// use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
				(currentClientValue[this.sabloConverters.INTERNAL_IMPL] as ComponentTypeInternalState).fireChanges(notificationParamForListeners);
			}

			return newValue;
		}

		public updateAngularScope(clientValue: ComponentValue, componentScope: angular.IScope): void {
			if (clientValue) {
    			this.removeAllWatches(clientValue);

                const internalState: ComponentTypeInternalState = clientValue[this.sabloConverters.INTERNAL_IMPL];
				if (internalState) {
					// update scope for non-foundset linked (or all in case comp. is not foundset linked) model properties
					const staticClientSideTypes = this.typesRegistry.getComponentSpecification(clientValue.componentDirectiveName);
					const beanModel = clientValue[ComponentType.MODEL_KEY];
					for (const key in beanModel) {
						let clientSideType:sablo.IType<any> = (<sablo.IType<any>> internalState.dynamicTypesForNonViewportProperties[key]);
						if (!clientSideType && staticClientSideTypes) clientSideType = staticClientSideTypes[key];
						
						if (clientSideType) clientSideType.updateAngularScope(beanModel[key], componentScope);
					}

					// update scope for any foundset linked properties in the viewport
					this.viewportModule.updateAngularScope(clientValue[ComponentType.MODEL_VIEWPORT], internalState, componentScope, false);
				}

    			if (componentScope) this.addBackWatches(clientValue, componentScope, internalState?.getModelPropertyChangeNotifierGenerator());  
			}
		}

		public fromClientToServer(newClientData: any, oldClientData: ComponentValue, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any {
			if (newClientData) {
				const internalState: ComponentTypeInternalState = newClientData[this.sabloConverters.INTERNAL_IMPL];
				if (internalState.isChanged()) {
					const tmp = internalState.requests;
					internalState.requests = [];
					return tmp;
				}
			}
			return [];
		}
		
	}
	
    // this class if currently copied over to all places where it needs to be implemented to avoid load order problems with js file that could happen on ng1 (like foundset or component type files being loaded in browser before viewport => runtime error because this class could not be found)
    // make sure you keep all the copies in sync    
    // ng2 will be smarter about load order and doesn't have to worry about this
    abstract class InternalStateForViewport_copyInComponentType implements sablo.ISmartPropInternalState {
        forFoundset?: () => FoundsetValue;
        
        viewportTypes: any; // TODO type this
        unwatchData: { [idx: number]: Array<() => void> };
        
        requests: Array<any> = [];
        
        // inherited from sablo.ISmartPropInternalState
        changeNotifier: () => void;
        
        
        constructor(public readonly webSocket: sablo.IWebSocket,
                        public componentScope: angular.IScope, public readonly changeListeners: Array<(values: any) => void> = []) {}
        
        getComponentScope() {
            return this.componentScope;
        }

        updateAngularScope(newComponentScope: angular.IScope) {
            this.componentScope = newComponentScope;
        }

        setChangeNotifier(changeNotifier: () => void): void {
            this.changeNotifier = changeNotifier;
        }
         
        isChanged(): boolean {
            return this.requests && (this.requests.length > 0);
        }
        
        fireChanges(changes: any) {
            for (let i = 0; i < this.changeListeners.length; i++) {
                this.webSocket.setIMHDTScopeHintInternal(this.componentScope);
                this.changeListeners[i](changes);
                this.webSocket.setIMHDTScopeHintInternal(undefined);
            }
        }

    }
    
    class ComponentTypeInternalState extends InternalStateForViewport_copyInComponentType {

        // just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
        beanLayout: any = {}; // not really useful right now; just to be able to reuse existing form code
        
        modelUnwatch: (() => void)[] = null;
        
        dynamicTypesForNonViewportProperties: object = { }; // here we will keep any dynamic client side types for model properties (not viewport model props., those are handled by viewport.ts)
        
        private readonly changeNotifierGenerator: sablo.PropertyChangeNotifierGeneratorFunction;

        constructor(private readonly componentValue: ComponentValue,
                        oldClientValueInternalState: ComponentTypeInternalState,
                        public readonly propertyContextCreator: sablo.IPropertyContextCreator,
                        componentScope: angular.IScope,
                        private readonly sabloConverters: sablo.ISabloConverters,
                        private readonly viewportModule: ngclient.propertyTypes.ViewportService,
                        webSocket: sablo.IWebSocket,
                        private readonly typesRegistry: sablo.ITypesRegistry,
                        private readonly sabloApplication: sablo.ISabloApplication) {

            super(webSocket, componentScope,
                // even if it's a completely new value, keep listeners from old one if there is an old value
                oldClientValueInternalState ? oldClientValueInternalState.changeListeners : []);
                
            // changeNotifierGenerator is only for model properties, viewport properties are handled in viewport.ts.
            this.changeNotifierGenerator = /*beanPropertyChangeNotifierGenerator*/ (propertyName: string): ((oldValue?: any, newValue?: any, dumb?: boolean) => void) => {
                return /*beanPropertyChangeNotifier*/ (oldValue, newValue, dumb) => { // oldValue, newValue and dumb are only set when called from bean model in-depth/shallow watch; not set for smart properties
                    if (dumb !== true) {
                        // so smart property - no watch involved (it notifies itself as changed)
                        oldValue = newValue = this.componentValue[ComponentType.MODEL_KEY][propertyName];
                    } 
                    this.requests.push({ propertyChanges : this.getChildPropertyChanges(oldValue, newValue, propertyName) });
                    if (this.changeNotifier) this.changeNotifier();
                };
            };
        };
        
        /** Works only for model properties, not for viewport model properties. */
        protected getClientSideTypeOfModelProp(propertyName: string): sablo.IType<any> {
            let clientSideType: sablo.IType<any> = this.dynamicTypesForNonViewportProperties[propertyName]; // try dynamic types for non-viewport props.
            if (!clientSideType) { // try static types for props.
                const staticClientSideTypes = this.typesRegistry.getComponentSpecification(this.componentValue.componentDirectiveName);
                if (staticClientSideTypes) clientSideType = staticClientSideTypes.getPropertyType(propertyName);
            }
            
            return clientSideType;
        }
        
        /** Works for both model properties and viewport model properties */
        getClientSideType(propertyName: string, rowId: any): sablo.IType<any> {
            let clientSideType: sablo.IType<any> = this.getClientSideTypeOfModelProp(propertyName);
            if (!clientSideType) clientSideType = this.viewportModule.getClientSideTypeFor(rowId, propertyName, this); // try viewport dynamic types
            
            return clientSideType;
        }
        
        /** Only for model properties, viewport properties are handled in viewport.ts. */
        getModelPropertyChangeNotifierGenerator(): sablo.PropertyChangeNotifierGeneratorFunction {
            return this.changeNotifierGenerator;
        }
    
        private getChildPropertyChanges(oldPropertyValue: any, newPropertyValue: any, propertyName: string) {
            // just dummy stuff - currently the parent controls layout, but getComponentChanges needs such args...
            const containerSize = {width: 0, height: 0};
    
            return this.sabloApplication.getComponentPropertyChange(newPropertyValue, oldPropertyValue,
                    this.getClientSideTypeOfModelProp(propertyName), propertyName, this.getComponentScope(),
                    this.propertyContextCreator.withPushToServerFor(propertyName), this.getModelPropertyChangeNotifierGenerator());
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
        private __internalState: ComponentTypeInternalState;
		
		constructor (serverJSONValue: object, oldClientValue: ComponentValue, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext,
					sabloConverters: sablo.ISabloConverters,
					sabloUtils: sablo.ISabloUtils,
					sabloService,
					uiBlocker,
					foundsetTypeConstants: foundsetType.FoundsetTypeConstants,
					viewportModule: ngclient.propertyTypes.ViewportService,
					q: angular.IQService,
					webSocket: sablo.IWebSocket,
					servoyInternal,
					typesRegistry: sablo.ITypesRegistry,
					sabloApplication: sablo.ISabloApplication,
					pushToServerUtils: sablo.IPushToServerUtils,
					log: sablo.ILogService) {

			for (const propName of Object.getOwnPropertyNames(serverJSONValue)) {
				this[propName] = serverJSONValue[propName];
			}

			const componentSpecification = typesRegistry.getComponentSpecification(this.componentDirectiveName);
			sabloConverters.prepareInternalState(this, new ComponentTypeInternalState(this, 
                    oldClientValue?.__internalState,
                    pushToServerUtils.newRootPropertyContextCreator((propertyName: string): any => {
                        return this[ComponentType.MODEL_KEY][propertyName]; // TODO will this ever need to look inside model viewport as well (so some properties to search for foundset linked properties on which they depend... I think currently this is not needed)
                    }, componentSpecification),
                    componentScope,
                    sabloConverters,
                    viewportModule,
                    webSocket,
                    typesRegistry,
                    sabloApplication));

			let internalState: ComponentTypeInternalState = this.__internalState;
			
			const modelPropChangedNotifierGenerator = internalState.getModelPropertyChangeNotifierGenerator();

			if (angular.isDefined(this[foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
				// if it's linked to a foundset, keep that info in internal state; viewport.js needs it
				const forFoundsetPropertyName = this[foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
				internalState.forFoundset = () => {
					return propertyContext.getProperty(forFoundsetPropertyName);
				};
				delete this[foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
			}
			const executeHandler = (handlerName, args, row, name, model) => {		
				const handlerSpec = typesRegistry.getComponentSpecification(this.componentDirectiveName)?.getHandler(handlerName);
				if ((!handlerSpec || !handlerSpec.ignoreNGBlockDuplicateEvents) && uiBlocker.shouldBlockDuplicateEvents(name, model, handlerName, row))
				{
					// reject execution
					log.info("rejecting duplicate event execution of: " + handlerName + " on " + name + " row " + row);
					return q.resolve(null);
				}
				
				const promiseAndCmsid = sabloService.createDeferedEvent();
				
				const newargs = sabloUtils.getEventArgs(args, handlerName, handlerSpec); // converts args for being sent to server
				internalState.requests.push({ handlerExec: {
					eventType: handlerName,
					args: newargs,
					rowId: row,
					defid: promiseAndCmsid.defid
				}});
				
				if (internalState.changeNotifier) internalState.changeNotifier();
				promiseAndCmsid.promise.finally(() => { uiBlocker.eventExecuted(name, model, handlerName, row); });
				return promiseAndCmsid.promise.then((retVal) => {
					return sabloConverters.convertFromServerToClient(retVal, handlerSpec?.returnType,
					       undefined, undefined, undefined, undefined, sabloUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES);
				});
			};

			// calling applyBeanData initially to make sure any needed conversions are done on model's properties
			const beanData = this[ComponentType.MODEL_KEY];
			const beanModel = new sablo_app.Model();
			this[ComponentType.MODEL_KEY] = beanModel;

			const containerSize = {width: 0, height: 0};

			servoyInternal.applyBeanData(beanModel, internalState.beanLayout, beanData, containerSize, modelPropChangedNotifierGenerator,
					this.componentDirectiveName, internalState.dynamicTypesForNonViewportProperties, componentScope);

			// component property is now be able to send itself entirely at runtime; we need to handle viewport conversions here as well
			const wholeViewport = this[ComponentType.MODEL_VIEWPORT_KEY];
			delete this[ComponentType.MODEL_VIEWPORT_KEY];
			this[ComponentType.MODEL_VIEWPORT] = [];

			if (wholeViewport) {
				this[ComponentType.MODEL_VIEWPORT] = viewportModule.updateWholeViewport(this[ComponentType.MODEL_VIEWPORT],
						internalState, wholeViewport, this[sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY],
						componentSpecification, componentScope,
						internalState.propertyContextCreator, false);
			}
			if (angular.isDefined(this[sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY])) delete this[sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY];

			if (!this.api) this.api = {};
			if (this.handlers)
			{
				for (const key in this.handlers) 
				{
					((key) => {
						const eventHandler = (args, rowId) => {
							return executeHandler(key, args, rowId, this.name, this.model);
						}
						const wrapper = function() {
							return eventHandler(arguments, null);
						}
						wrapper.selectRecordHandler = (rowId) => {
							return function() { 
								return eventHandler(arguments, rowId instanceof Function ? rowId() : rowId) 
							}
						};
						this.handlers[key] = wrapper;
					})(key);
				}
			}

			// here we don't specify any of the following as all those can be forwarded by the parent component from it's own servoyApi:
			// formWillShow, hideForm, getFormUrl, isInDesigner, getFormComponentElements, 
			this.servoyApi = {
				/** rowId is only needed if the component is linked to a foundset */
				startEdit: (property, rowId) => {
					const req = { svyStartEdit: {} };

					if (rowId) req.svyStartEdit[foundsetTypeConstants.ROW_ID_COL_KEY] = rowId;
					req.svyStartEdit[ComponentType.PROPERTY_NAME_KEY] = property;

					internalState.requests.push(req);
					if (internalState.changeNotifier) internalState.changeNotifier();
				},

				apply: (property, modelOfComponent, rowId) => {
					/** rowId is only needed if the component is linked to a foundset */
					if (!modelOfComponent) modelOfComponent = this[ComponentType.MODEL_KEY]; // if it's not linked to foundset componentModel will be undefined
					let propertyValue = modelOfComponent[property];

					let clientSideType: sablo.IType<any> = internalState.getClientSideType(property, rowId);
					
					propertyValue = sabloConverters.convertFromClientToServer(propertyValue, clientSideType, undefined, componentScope,
					{
						getProperty: (propertyName: string) => {
							return modelOfComponent ? modelOfComponent[propertyName] : undefined;
						},
						getPushToServerCalculatedValue: () => {
							return componentSpecification.getPropertyPushToServer(property);
						}
					} as sablo.IPropertyContext); // even if clientSideType is still undefined, do the default conversion

					const req = { svyApply: {} };

					if (rowId) req.svyApply[foundsetTypeConstants.ROW_ID_COL_KEY] = rowId;
					req.svyApply[ComponentType.PROPERTY_NAME_KEY] = property;
					req.svyApply[ComponentType.VALUE_KEY] = propertyValue;

					internalState.requests.push(req);
					if (internalState.changeNotifier) internalState.changeNotifier();
				}
			}

		}
	
		/**
		 * Adds a change listener that will get triggered when server sends granular or full modelViewport changes for this component.
		 * 
		 * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this same foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
		 * @param viewportChangeListener the listener to register.
		 * 
		 * @return a listener unregister function.
		 */
		public addViewportChangeListener(viewportChangeListener : componentType.ViewportChangeListener) : () => void {
			this.__internalState.changeListeners.push(viewportChangeListener);
			return () => { this.removeViewportChangeListener(viewportChangeListener); };
		}
		
		public removeViewportChangeListener(viewportChangeListener : componentType.ViewportChangeListener) : void {
			const index = this.__internalState.changeListeners.indexOf(viewportChangeListener);
			if (index > -1) {
				this.__internalState.changeListeners.splice(index, 1);
			}
		}
		
	}
	
}
