/// <reference path="../angularjs/angular.d.ts" />

declare namespace sablo {
	
	interface ILogService extends angular.ILogService {
		debugEnabled: boolean;
		debugLevel: number;
		DEBUG: number;
		SPAM: number;
	}
	
	interface FormState {
		model:any;
		api:any;
		properties: any;
		initializing:boolean;
		layout?:any;
		style?:any;
		initialDataRequested?: boolean;
		blockPostLinkInHidden?: boolean;
		resolving?: boolean;
		resolved?: boolean;
		getScope?(): angular.IScope;
		addWatches?():void			
	}
	
	interface WSSession {
		callService<T>(serviceName:string, methodName:string, argsObject, async:boolean):angular.IPromise<T>;
		sendMessageObject:()=>void;
		onopen:(handler:(evt)=>void)=>void;
		onerror:()=>void;
		onclose:()=>void;
		onMessageObject(handler:(msg, conversionInfo, scopesToDigest: ScopeSet)=>void): void; 
	}
	
	interface Locale {
		language:string;
		country:string;
		full: string;
	}
	
	interface SabloConstants {
		modelChangeNotifier:string,
	}
	
	interface ISabloApplication {
        connect(context, queryArgs, websocketUri): WSSession;
        contributeFormResolver(contributedFormResolver:{prepareUnresolvedFormForUse(form:string)}): void;
        getClientnr(): string;
        getSessionId(): string;
        getWindowName(): string;
        getWindownr(): string;
        getWindowUrl(name:string): string;
        applyBeanData(beanModel, beanData, containerSize, changeNotifierGenerator, componentSpecName: string, componentScope:angular.IScope): void;
        getComponentChanges(now, prev, componentSpecName: string, parentSize, property): any;
        getChangeNotifierGenerator(formName:string, beanName:string):(property:string)=>void;
        getFormState(name:string): angular.IPromise<FormState>;
        getFormStateWithData(name:string): angular.IPromise<FormState>;
        getFormStateEvenIfNotYetResolved(name:string): FormState;
        getFormStatesConversionInfo(): any;
		hasFormState(name:string): boolean;
        hasResolvedFormState(name:string): boolean;
        hasFormStateWithData(name:string):boolean;
        clearFormState(name:string): void;
        initFormState(formName, beanDatas, formProperties, formScope, resolve): FormState;
        updateScopeForState(formName: string, formScope: angular.IScope, state: FormState): void;
        resolveFormState(formName:string, skipTestResolving:boolean): FormState;
		resolveFormState(formName:string): FormState;
        unResolveFormState(formName:string): void;
        requestInitialData(formName:string, requestDataCallback:(initialFormData:any, formState:FormState)=>void): void;
        sendChanges(now, prev, formname:string, beanname:string, property:string): void;
        callService<T>(serviceName:string, methodName:string, argsObject, async:boolean): angular.IPromise<T>;
		callService<T>(serviceName:string, methodName:string, argsObject): angular.IPromise<T>;
        addToCurrentServiceCall(func:()=>void): void;
        getExecutor(formName:string):{on:(beanName:string, eventName:string, property:string, args, rowId:string)=>void};
        getLanguageAndCountryFromBrowser(): string;
        getLocale(): Locale;
        setLocale(locale:Locale): void;
        getCurrentFormUrl(fetch:boolean): string;
        setCurrentFormUrl(url:string, push:boolean): void;
	}
	
	interface ISabloConverters {
        INTERNAL_IMPL: string,
//        TYPES_KEY: string,
        CONVERSION_CL_SIDE_TYPE_KEY: string,
        prepareInternalState(propertyValue, optionalInternalStateValue?):void
        convertFromServerToClient(serverSentData: any, typeOfData: sablo.IType<any>, currentClientData: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any,
        convertFromClientToServer(newClientData: any, typeOfData: sablo.IType<any>, oldClientData: any): any,
//        updateAngularScope(value, conversionInfo, scope:angular.IScope): void,
//        registerCustomPropertyHandler(propertyTypeID:string, customHandler:{
//			fromServerToClient(serverJSONValue, currentClientValue, componentScope:angular.IScope, propertyContext:(propertyName: string)=>any):void,
//			fromClientToServer(newClientData, oldClientData):void,
//			updateAngularScope(clientValue, componentScope:angular.IScope):void
//		},overwrite:boolean): void
	}
	
	interface ISabloUtils {
		EVENT_LEVEL_SYNC_API_CALL: number,
		DEFAULT_CONVERSION_TO_SERVER_FUNC: string,
		setCurrentEventLevelForServer(eventLevelValue:number): void,
		getCurrentEventLevelForServer():number,
		isChanged(now, prev, clientSideType: IType<any>):boolean,
		getCombinedPropertyNames(now,prev): any,
//		convertClientObject(value):any,
		getEventArgs(args,eventName:string):any,
		
		/**
		 * Receives variable arguments. First is the object obj and the others (for example a, b, c) are used to
		 * return obj[a][b][c] making sure that if any does not exist or is null (for example b) it will be set to {}.
		 */
		getOrCreateInDepthProperty(object: any, ...pathOfSubpropertyNames: string[]):any,
		
		/**
		 * Receives variable arguments. First is the object obj and the others (for example a, b, c) are used to
		 * return obj[a][b][c] making sure that if any does not exist or is null it will just return null/undefined instead of erroring out.
		 */
		getInDepthProperty(object: any, ...pathOfSubpropertyNames: string[]):any,
		
		cloneWithDifferentPrototype(obj:Object, newPrototype:Object):Object
	}
	
	interface IWebSocket {
        connect(context:string, args, queryArgs, websocketUri:string):WSSession,
        setConnectionQueryArgument(arg:string, value:string):void,
        setConnectionPathArguments(args):void,
        getSession():WSSession,
        isConnected(): boolean,
        isReconnecting(): boolean,
        
    	/**
    	 * For internal use. Only used in order to implement backwards compatibility with addIncomingMessageHandlingDoneTask tasks that do not return the touched/modified scopes.
    	 * In that case we try to guess those scopes via this method that should be called whenever we know code executing on a specific scope might call addIncomingMessageHandlingDoneTask(...).
    	 */
    	setIMHDTScopeHintInternal(scope: angular.IScope),
    	
    	/**
    	 * Wait for all incoming changes to be applied to properties first (if a message from server is currently being processed) before executing given function
    	 * 
    	 * @param func will be called once the incoming message from server has been processed; can return an array of angular scopes that were touched/changed by this function; those scopes will be
    	 * digested after all "incomingMessageHandlingDoneTask"s are executed. If no scope was altered you should return []. If the function returns nothing then sablo tries to detect situations when
         * the task is added while some property value change from server is being processed and to digest the appropriate scope afterwards...
    	 */
    	addIncomingMessageHandlingDoneTask(func: () => angular.IScope[]): void,
    	
        disconnect():void,
        getURLParameter(name:string): string,
        setPathname(pathname: string): void,
        getPathname(): string,
        setQueryString(queryString: string): void,
        getQueryString(): string
	}
	
	interface ISabloDeferHelper {
		initInternalStateForDeferring(internalState, timeoutRejectLogPrefix?: string): void
		initInternalStateForDeferringFromOldInternalState(internalState, oldInternalState): void
		getNewDeferId(internalState): number
		retrieveDeferForHandling(msgId: number, internalState):angular.IDeferred<any>
		cancelAll(internalState):void
	}
	
	interface ISabloLoadingIndicator {
		showLoading(): void;
		hideLoading(): void;
		isShowing(): boolean;
		isDefaultShowing(): boolean;
	}
	
	interface HashCodeFunc<T> {
	    (T: any): number;
	}

	/** A custom 'hash set' based on a configurable hash function received in constructor for now it can only do putItem */
	class CustomHashSet<T> {
		constructor(hashCodeFunc: HashCodeFunc<T>);
		
		public putItem(item: T) : void;
		
		public hasItem(item: T) : boolean;
		
		public getItems() : Array<T>;
	}

	/** A CustomHashSet that uses as hashCode for angular scopes their $id. */
	class ScopeSet extends CustomHashSet<angular.IScope> {
		constructor();		
	}
	
	/**
	 * The types registry holds information about all service client side types (for properties, api params/return value) and about all needed component client side types (for properties, apis, handlers).
	 * Client side types are those types that require client side conversions to/from server.
	 */
	interface ITypesRegistry {
		
		getTypeFactoryRegistry(): ITypeFactoryRegistry;
		registerGlobalType(typeName: string, theType: IType<any>): void;

	}
	
	interface ITypesRegistryForTypeFactories extends ITypesRegistry {

		/**
		 * This method is to be used outside of the type registry only by ITypeFactory instances that need to get IType instances from ITypeFromServer
		 * when the ITypeFactory.registerDetails(...) method is called.
		 * All other code already has IType instances available (not ITypeFromServer) and does not need this.
		 * 
		 * @param typeFromServer the type as it was received from server.
		 * @param webObjectSpecName the name of the component/service that it was received for.
		 */
		processTypeFromServer(typeFromServer: ITypeFromServer, webObjectSpecName: string): IType<any>;

	}

	interface ITypesRegistryForSabloConverters extends ITypesRegistry {

		/**
		 * This method returns only simple (non-factory) types that are already registered with the type registry. It should only be used from $sabloConverters.convertFromServerToClient(...) or
		 * types that inside their impl. can send variable nested types (for instance 'object' type can have random 'date' values nested in it).
		 * All other code already has IType instances available (not ITypeFromServer) and does not need this.
		 * 
		 * @param typeFromServer the type as it was received from server.
		 */
		getAlreadyRegisteredType(typeFromServer: ITypeFromServer): IType<any>;

	}

	/**
	 * An IType is a type of data (properties/arguments/return values) that requires client side client-to-server and server-to-client conversions.
	 * VT is the client side type of value for that property.
	 */
	interface IType<VT> {
		
		/**
		 * Converts the JSON value received from server for this type of property into a client-side value specific for this type and returns that.
		 * 
		 * @param serverJSONValue can be any JSON valid value (primitive, object, ...)
		 * @param currentClientValue the current value that this property had (if any) on client before the new value arrived; this is useful sometimes in case of component/service properties.
		 *                           In all other cases (args, return values) it's null/undefined.
		 * @param componentScope an angular scope (of the component/service) that this conversion should use if a scope is needed (for watches for example).
		 *                       It can be null/undefined if conversion happens for service/component API call parameters for example.
		 * @param propertyContext (useful for properties of components/services) a way for this property to access another property in the current property context (if in the root of the web
		 *                        object then other root properties, if in a nested custom object - other properties in the same custom object with fallback to parent level property context).
		 *                        It can be null/undefined if conversion happens for service/component API call parameters for example.
		 *                        
		 * @return the new or updated client side property value; if this returned value is interested in being able to triggering sending updates to server when something changes client side in it
		 *         it must have these member functions in it's [$sabloConverters.INTERNAL_IMPL]: // TODO change all this to a typescript interface ISmartPropertyValue and define a type for changeNotifier maybe
		 *	           setChangeNotifier: function(changeNotifier) - where changeNotifier is a function that can be called when
		 *                                                         the value needs to send updates to the server; this method will
		 *                                                         not be called when value is a call parameter for example, but will
		 *                                                         be called when set into a component's/service's property/model
		 *             isChanged: function() - should return true if the value needs to send updates to server
		 */
        fromServerToClient(serverJSONValue: any, currentClientValue: VT, componentScope: angular.IScope, propertyContext: IPropertyContext): VT;
        
        /**
         * Converts a client side value to a corresponding JSON value that is to be sent to server.
         * 
         * @param newClientData the client data to be converted for sending to server. It's not typed VT in case of values that can be completely created clientside (for example arrays/objects and that can be set without yet being the correct instance).
         * @param oldClientData (for properties) in case the value of this property has changed by reference - the old value of this property; it can be null/undefined if
		 *                      conversion happens for service API call parameters for example...
		 *                      
		 * @return the JSON value to send to the server
         */
        fromClientToServer(newClientData: any, oldClientData: VT): any;
        
        /**
         * (for properties) Because some forms might become invisible and then visible again keeping the same data in the model, all properties are notified via this method if the angular scope
         * that they received in fromServerToClient is no longer available or if this property value should be linked to another(new) angular scope.
         * 
         * @param clientValue the client side value of this property.
         * @param componentScope the new angular scope (can be null/undefined if form was hidden, in which case the property type should do any needed cleanup operations on previous scope).
         */
        updateAngularScope(clientValue: VT, componentScope: angular.IScope): void;
		
	}
	
	type IPropertyContext = (propertyName: string) => any;

	/** The type definition with client side conversion types for a component or service.  */
	interface IWebObjectSpecification {

		getPropertyType(propertyName:string): IType<any>;
		getHandler(handlerName:string): IWebObjectFunction;
		getApiFunction(apiFunctionName:string): IWebObjectFunction;

	}
	
	/** The type definition with client side conversion types for a handler or api function.  */
	interface IWebObjectFunction {

		readonly returnType?: IType<any>;
		getArgumentType(argumentIdx:number): IType<any>;

	}
	
	/**
	 * This is what server sends for a type (either a simple global type or a tuple for factory types). This type is only to be used in type registry code or code
	 * that processes server-side sent types such as ITypeFactory.registerDetails to get the client-side IType instances from that.
	 */
	export type ITypeFromServer = string | [string, string];
	
	/**
	 * Factory types (custom objects for instance) are registered and used through this registry. For example a custom object type is not just a type,
	 * it has a specific declaration for sub-property types based on each individual custom object type from spec. So a factory would create all these specific custom object types.
	 */
	interface ITypeFactoryRegistry {

		getTypeFactory(typeFactoryName: string): ITypeFactory<any>;
		contributeTypeFactory(typeFactoryName: string, typeFactory: ITypeFactory<any>);
		
	}
	
	/**
	 * See ITypeFactoryRegistry description. Some types like custom objects need to create more specific types for actual usage. For example a
	 * custom object is different based on how it is defined in it's .spec file.
	 * 
	 * VT is the client side type of value that specific types created by this factory will use.
	 */
	interface ITypeFactory<VT> {

		/**
		 * Asks the factory to get (if it has already created this specific (sub)type) or create a specific type with the given specificTypeInfo (could be a custom object type name from spec or
		 * in case of arrays it could be the type name of elements). Some type factories will have to rely on previously registered details for that specificTypeInfo that can be received from
		 * server via registerDetails(...).
		 * 
		 * IMPORTANT: It is the responsibility of this factory to cache any newly created specific types as needed.
		 * 
		 * @param specificTypeInfo the information that can make a specific type from this factory of types (could be for example a custom object type name from spec or in case of arrays it could be the type name of elements)
		 * @param webObjectSpecName as types are/can be scoped inside a web object (component or service) .spec we also give the webObjectSpecName here.
		 * 
		 * @returns the specific type for the given arguments.
		 */
		getOrCreateSpecificType(specificTypeInfo: string, webObjectSpecName: string): IType<VT>;
		
		/**
		 * Gives the factory details that are needed for it to be able to create needed specific (sub)types. 
		 * @param details for example is case of a JSON_obj factory the details would be the types of it's child properties. (ICustomTypesFromServer)
		 * @param webObjectSpecName the web object for which this details were sent from the server.
		 */
		registerDetails(details: any, webObjectSpecName: string): void;

	}

}