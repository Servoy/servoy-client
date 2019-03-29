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
		modelChangeNotifier:string	
	}

	interface ISabloApplication {
        connect(context, queryArgs, websocketUri): WSSession;
        contributeFormResolver(contributedFormResolver:{prepareUnresolvedFormForUse(form:string)}): void;
        getSessionId(): string;
        getWindowName(): string;
        getWindowNr(): string;
        getWindowUrl(name:string): string;
        applyBeanData(beanModel, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope:angular.IScope):void ;
        getComponentChanges(now, prev, beanConversionInfo, parentSize, property): any;
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
        clearSabloSession():void;
	}
	
	interface ISabloConverters {
        INTERNAL_IMPL: string,
        TYPES_KEY: string,
        prepareInternalState(propertyValue, optionalInternalStateValue):void
        convertFromServerToClient(serverSentData:any, conversionInfo:any, currentClientData:any, scope:angular.IScope, propertyContext:(propertyName: string)=>any): any,
        convertFromClientToServer(newClientData:any, conversionInfo:any, oldClientData:any): any,
        updateAngularScope(value, conversionInfo, scope:angular.IScope): void,
        registerCustomPropertyHandler(propertyTypeID:string, customHandler:{
			fromServerToClient(serverJSONValue, currentClientValue, componentScope:angular.IScope, propertyContext:(propertyName: string)=>any):void,
			fromClientToServer(newClientData, oldClientData):void,
			updateAngularScope(clientValue, componentScope:angular.IScope):void
		},overwrite:boolean): void 
	}
	
	interface ISabloUtils {
			EVENT_LEVEL_SYNC_API_CALL: number,
			setCurrentEventLevelForServer(eventLevelValue:number): void,
			getCurrentEventLevelForServer():number,
			isChanged(now, prev, conversionInfo):boolean,
			getCombinedPropertyNames(now,prev): any,
			convertClientObject(value):any,
			getEventArgs(args,eventName:string):any,
			getOrCreateInDepthProperty(formStatesConversionInfo, formname:string, beanname:string):any,
			getInDepthProperty(formStatesConversionInfo, formname:string, beanname:string):any,
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

}