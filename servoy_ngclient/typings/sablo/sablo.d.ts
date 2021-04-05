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
        addWatches?(beanNames?: string[]):void;
        removeWatches?(beanNames?: string[]):void;
        componentSpecNames: { [componentName: string]: string };
    }
    
    type MessageObjectHandler = (msg, scopesToDigest: ScopeSet) => angular.IPromise<any> | void;
    
    interface WSSession {
        callService<T>(serviceName:string, methodName:string, argsObject, async:boolean):angular.IPromise<T>;
        sendMessageObject:()=>void;
        onopen:(handler:(evt)=>void)=>void;
        onerror:()=>void;
        onclose:()=>void;
        onMessageObject(messageObjectHandler: MessageObjectHandler): void; 
    }
    
    interface Locale {
        language:string;
        country:string;
        full: string;
    }
    
    interface SabloConstants {
        modelChangeNotifier:string,
    }
    
    /**
     * oldValue, newValue and dumb are only set when called from bean model in-depth/shallow watch; not set for smart properties
     */
    type PropertyChangeNotifierGeneratorFunction = (propertyName: string) => ((oldValue?: any, newValue?: any, dumb?: boolean) => void);
    
    interface ISabloApplication {
        connect(context, queryArgs, websocketUri): WSSession;
        contributeFormResolver(contributedFormResolver:{prepareUnresolvedFormForUse(form:string)}): void;
        getClientnr(): string;
        getSessionId(): string;
        getWindowName(): string;
        getWindownr(): string;
        getWindowUrl(name:string): string;
        applyBeanData(beanModel: any, beanData: any, containerSize: any, changeNotifierGenerator:PropertyChangeNotifierGeneratorFunction,
                componentSpecName: string, dynamicPropertyTypesHolder: object, componentScope:angular.IScope): void;
        getComponentPropertyChange(now: any, prev: any, typeOfProperty: sablo.IType<any>, propertyName: string, scope: angular.IScope,
                propertyContext: sablo.IPropertyContext, changeNotifierGeneratorFunction: sablo.PropertyChangeNotifierGeneratorFunction): any;
        getAllChanges(now: object, prev: object, dynamicTypes: object, scope: angular.IScope, propertyContextCreator: sablo.IPropertyContextCreator): any;
        getChangeNotifierGenerator(formName:string, beanName:string):sablo.PropertyChangeNotifierGeneratorFunction;
        getFormState(name:string): angular.IPromise<FormState>;
        getFormStateWithData(name:string): angular.IPromise<FormState>;
        getFormStateEvenIfNotYetResolved(name:string): FormState;
        getFormStatesDynamicClientSideTypes(): any;
        hasFormState(name:string): boolean;
        hasResolvedFormState(name:string): boolean;
        hasFormStateWithData(name:string):boolean;
        clearFormState(name:string): void;
        initFormState(formName, beanDatas, componentSpecNames: { [componentName: string]: string }, formProperties, formScope, resolve): FormState;
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

        CONVERSION_CL_SIDE_TYPE_KEY: string,
        VALUE_KEY: string,
        prepareInternalState(propertyValue, optionalInternalStateValue?):void
        /**
         * @see sablo.IType#fromServerToClient; this method only adds on top of that some dynamic type conversions and some default conversions in case the given type is undefined.
         */
        convertFromServerToClient(serverSentData: any, typeOfData: sablo.IType<any>, currentClientData: any,
                dynamicPropertyTypesHolder: object /* can be null; some types decide at runtime the type needed on client - for example dataprovider type could send date, and we will store that info here*/,
                keyForDynamicTypes: string, /* can be null */
                scope: angular.IScope, propertyContext: sablo.IPropertyContext): any,
        /**
         * @see sablo.IType#fromClientToServer; this method only adds on top of that some default conversions in case given type is undefined.
         */
        convertFromClientToServer(newClientData: any, typeOfData: sablo.IType<any>, oldClientData: any, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any,

    }
    
    interface ISmartPropInternalState {
        changeNotifier: () => void;
        setChangeNotifier(changeNotifier: () => void): void;
        isChanged(): boolean;
    }
    
    interface IInternalStateWithDeferred {
        deferred: { [msgId: number]: { defer: angular.IDeferred<unknown>, timeoutPromise: angular.IPromise<void> } }; // key is msgId (which always increases), values is { defer: ...q defer..., timeoutPromise: ...timeout promise for cancel... }
        currentMsgId: number;
        timeoutRejectLogPrefix: string;
    }

    interface ISabloUtils {
        EVENT_LEVEL_SYNC_API_CALL: number,

        PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES: IPropertyContext,
        PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES: IPropertyContext,
            
        setCurrentEventLevelForServer(eventLevelValue:number): void,
        getCurrentEventLevelForServer():number,
        isChanged(now, prev, clientSideType: IType<any>):boolean,
        getCombinedPropertyNames(now,prev): any,
//      convertClientObject(value):any,
        getEventArgs(args, eventName:string, handlerSpecification: sablo.IWebObjectFunction):any,
        
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
     * The types registry holds information about all service client side types (for properties, api params/return value) and about all needed
     * component client side types (for properties, apis, handlers).
     * Client side types are those types that require client side conversions to/from server.
     */
    interface ITypesRegistry {
        
        getTypeFactoryRegistry(): ITypeFactoryRegistry;
        registerGlobalType(typeName: string, theType: IType<any>, onlyIfNotAlreadyRegistered: boolean): void;

        getComponentSpecification(componentSpecName: string): IWebObjectSpecification;
        getServiceSpecification(serviceSpecName: string): IWebObjectSpecification;

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
        
        /**
         * Similar to #processTypeFromServer(...) but it processes a property description not just a type; PDs can have both type and pushToServer values for a property.
         * @param propertyDescriptionFromServer what we received from server for a property description
         * @param webObjectSpecName the name of the component/service that it was received for.
         */
        processPropertyDescriptionFromServer(propertyDescriptionFromServer: IPropertyDescriptionFromServer, webObjectSpecName: string): IPropertyDescription;

    }

    interface ITypesRegistryForSabloConverters extends ITypesRegistry {

        /**
         * This method returns only simple (non-factory) types that are already registered with the type registry. It should only be used from $sabloConverters.convertFromServerToClient(...) or
         * types that inside their impl. can send variable nested types (for instance 'object' type can have random 'date' values nested in it).
         * All other code already has IType instances available (not ITypeFromServer) via IWebObjectSpecification - and does not need this.
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
         *             setChangeNotifier: function(changeNotifier) - where changeNotifier is a function that can be called when
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
         * @param oldClientData (only for properties, not args/ret vals) in case the value of this property has changed by reference - the old value of this property; it can be null/undefined if
         *                      conversion happens for service API call parameters for example...
         * @param scope (only for properties, not args/ret vals) the component's or service's angular scope.
         * @param propertyContext (only for properties, not args/ret vals) a way for this property to access another property in the current property context (if in the root of the web
         *                        object then other root properties, if in a nested custom object - other properties in the same custom object with fallback to parent level property context).
         *                        It can be null/undefined if conversion happens for service/component API call parameters for example.
         * 
         * @return the JSON value to send to the server
         */
        fromClientToServer(newClientData: any, oldClientData: VT, scope: angular.IScope, propertyContext: sablo.IPropertyContext): any;
        
        /**
         * (for properties) Because some forms might become invisible and then visible again keeping the same data in the model, all properties are notified via this method if the angular scope
         * that they received in fromServerToClient is no longer available or if this property value should be linked to another(new) angular scope.
         * 
         * @param clientValue the client side value of this property.
         * @param componentScope the new angular scope (can be null/undefined if form was hidden, in which case the property type should do any needed cleanup operations on previous scope).
         */
        updateAngularScope(clientValue: VT, componentScope: angular.IScope): void;
        
    }
    
    type IPropertyContextGetterMethod = (propertyName: string) => any;
    
    /**
     * websocket.ts implements this in it's sablo.propertyTypes.PropertyContext class.
     */
    interface IPropertyContext {
        getProperty: IPropertyContextGetterMethod;
        getPushToServerCalculatedValue() : IPushToServerEnum;
    }
    
    interface IPropertyContextCreator {
        withPushToServerFor(propertyName: string): IPropertyContext;
    }
    
    // because Typescript d.ts files will not generate any Javascript amd compilation does not replace these with simple constants,
    // there is no way to use actual typescript enums in d.ts files without hacks/workarounds; so we can't make PushToServerEnum an actual enum
    // if we declared this enum here in the d.ts, it would compile but it would not be found at runtime... enums need to be in simple .ts files that
    // do generate javascript; there are a lot of discussions out there about how to work around this problem;
    // so we fake the enum here via types/interfaces/classes
    type PushToServerEnum_reject = 0; // default, throw exception when updates are pushed to server
    type PushToServerEnum_allow = 1; // allow changes, no default watch client-side
    type PushToServerEnum_shallow = 2; // allow changes, creates a watcher on client with objectEquality = false
    type PushToServerEnum_deep = 3; // allow changes, creates a watcher on client with objectEquality = true
    type PushToServerEnumValue = PushToServerEnum_reject | PushToServerEnum_allow | PushToServerEnum_shallow | PushToServerEnum_deep;
    
    /** see IPushToServerUtils.reject and other members for actual values of this enum */
    interface IPushToServerEnum {
        
        value: PushToServerEnumValue;
        combineWithChild(childDeclaredPushToServer: IPushToServerEnum): IPushToServerEnum;
        
    }
    
    interface IPushToServerUtils {
        
        // these will be singleton values that are instantiated through a private constructor on class PushToServerEnum which implements IPushToServerEnum 
        reject: IPushToServerEnum; // default, throw exception when updates are pushed to server
        allow: IPushToServerEnum; // allow changes, no default watch client-side
        shallow: IPushToServerEnum; // allow changes, creates a watcher on client with objectEquality = false
        deep: IPushToServerEnum; // allow changes, creates a watcher on client with objectEquality = true
        
        enumValueOf(pushToServerRawValue: PushToServerEnumValue): IPushToServerEnum;
        newRootPropertyContextCreator(getProperty: IPropertyContextGetterMethod, webObjectSpec: IWebObjectSpecification): IPropertyContextCreator;
        newChildPropertyContextCreator(getProperty: IPropertyContextGetterMethod,
                propertyDescriptions: { [propName: string]: sablo.IPropertyDescription },
                computedParentPushToServer: IPushToServerEnum): IPropertyContextCreator;
                
    }
    
    /** The type definition with client side conversion types for a component or service.  */
    interface IWebObjectSpecification {

        getPropertyDescription(propertyName:string): IPropertyDescription;
        getPropertyType(propertyName:string): IType<any>;
        /** This is the value of pushToServer as declared in the spec file... it can be undefined;
         * this value should be calculated from the parent properties using PushToServerUtil#combineWithChild, and root properties that do not
         * have it defined must be considered by default as PushToServerEnum.reject or call #getPropertyPushToServer(...) instead. */
        getPropertyDeclaredPushToServer(propertyName:string): IPushToServerEnum;
        /**
         * Same as #getPropertyDeclaredPushToServer(...) but if spec file does not declare a push to server value it will default to PushToServerEnum.reject
         * instead of returning undefined. Use this for root component/service properties.
         */
        getPropertyPushToServer(propertyName:string): IPushToServerEnum;

        getPropertyDescriptions(): { [propertyName: string]: IPropertyDescription };
        getHandler(handlerName:string): IWebObjectFunction;
        getApiFunction(apiFunctionName:string): IWebObjectFunction;

    }
    
    interface IPropertyDescription {

        getPropertyType(): IType<any>;
        
        /** This is the value of pushToServer as declared in the spec file... it can be undefined;
         * this value should be calculated from the parent properties using PushToServerUtil#combineWithChild, and root properties that do not
         * have it defined must be considered by default as PushToServerEnum.reject or call #getPropertyPushToServer() instead. */
        getPropertyDeclaredPushToServer(): IPushToServerEnum;

        /**
         * Same as #getPropertyDeclaredPushToServer() but if spec file does not declare a push to server value it will default to PushToServerEnum.reject
         * instead of returning undefined. Use this for root component/service properties.
         */
        getPropertyPushToServer(): IPushToServerEnum;

    }
    
    /** The type definition with client side conversion types for a handler or api function.  */
    interface IWebObjectFunction {

        readonly returnType?: IType<any>;
        getArgumentType(argumentIdx:number): IType<any>;

    }
    
    /**
     * This is what server sends for a type (either a simple global type or a tuple for factory types, the factory name and arg). This type is only
     * to be used in type registry code or code that processes server-side sent types such as ITypeFactory.registerDetails to get the client-side IType
     * instances from that.
     */
    type ITypeFromServer = string | [string, object];
    
    
    type IPropertyDescriptionFromServerWithMultipleEntries = { t?: ITypeFromServer, s: PushToServerEnumValue };
    
    /** Type and pushToServer for a property */
    type IPropertyDescriptionFromServer = ITypeFromServer | IPropertyDescriptionFromServerWithMultipleEntries;
    
    /**
     * Factory types (custom objects for instance) are registered and used through this registry. For example a custom object type is not just a type,
     * it has a specific declaration for sub-property types based on each individual custom object type from spec. So a factory would create all these specific custom object types.
     */
    interface ITypeFactoryRegistry {

        getTypeFactory(typeFactoryName: string): ITypeFactory<any>;
        contributeTypeFactory(typeFactoryName: string, typeFactory: ITypeFactory<any>): void;
        
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
        getOrCreateSpecificType(specificTypeInfo: any, webObjectSpecName: string): IType<VT>;
        
        /**
         * Gives the factory details that are needed for it to be able to create needed specific (sub)types. 
         * @param details for example is case of a JSON_obj factory the details would be the types of it's child properties. (ICustomTypesFromServer)
         * @param webObjectSpecName the web object for which this details were sent from the server.
         */
        registerDetails(details: any, webObjectSpecName: string): void;

    }
    
    interface IPropertyWatchUtils {
        
        /** returns an array of watch unregister functions */
        watchDumbPropertiesForComponent(scope: angular.IScope, componentTypeName: string,
                                          model: object, changedCallbackFunction: (newValue: any, oldValue:any, propertyName: string) => void): (() => void)[];
        
        /** returns an array of watch unregister functions */
        watchDumbPropertiesForService(scope: angular.IScope, serviceTypeName: string,
                                      model: object, changedCallbackFunction: (newValue: any, oldValue:any, propertyName: string) => void): (() => void)[];
        
    }

}